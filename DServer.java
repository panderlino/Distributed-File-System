
import spread.*;

import java.net.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.io.*;


public class DServer extends ServerProcess implements BasicMessageListener {
	
	private final String groupName = "SERVER_GROUP";

	// The Processes List from each process 
	public List<ServerProcess> processesList;
	
	// The Slaves List from each process
	public List<String> slavesList;
	
	// The queue of operations that will be execute
	public Queue<String> opsQueue;
	
	// The list containing files which are stored and in which slave
	// ex.: "file02.a#SLAVE_3" - file02.a is stored on SLAVE_3
	List<String> storedFilesList;
	
	// Atribute that says if this process is the master process from spreadGroup
	private boolean isMaster;

	// The Spread Connection.
	private SpreadConnection connection;
	
	// A spreadGroup.
	private SpreadGroup spreadGroup;

	private int currentSlave;
	
	
	// Print this membership data.  Does so in a generic way so identical
	// function is used in recThread and User. 
	private void handleMembershipInfo(MembershipInfo info) 
	{
	        //SpreadGroup spreadGroup = info.getGroup();
	 	//int auxint;
		if(info.isRegularMembership()) {
			//SpreadGroup members[] = info.getMembers();
			//MembershipInfo.VirtualSynchronySet virtual_synchrony_sets[] = info.getVirtualSynchronySets();
			//MembershipInfo.VirtualSynchronySet my_virtual_synchrony_set = info.getMyVirtualSynchronySet();

			System.out.print("\tDevido a ");
			if(info.isCausedByJoin()) {
				System.out.println("entrada de " + info.getJoined());
			}	else if(info.isCausedByLeave()) {
				System.out.println("saida de " + info.getLeft());
				// Tratar mensagem de um processo que foi cancelado
				
				
			}	else if(info.isCausedByDisconnect()) {
				
				// monta a mensagem pela desconexao
				System.out.println("desconexao de " + info.getDisconnected());
				String dado = info.getDisconnected() + "";
				String[] s = dado.split("#");
				
				int IDx = Integer.parseInt(s[1].substring(7));
				System.out.println("Remover o processo: " + IDx);
				ServerProcess p = new ServerProcess(IDx, 0);
				
				removeProcess(p);
				election();
				
				// Imprime a lista de processos
				System.out.print("\nLista de processos atual: \n\t");
				printProcessesList();

				
			}
		}
	}
	
	private void resolveMessage(SpreadMessage msg)
	{
		

		
		try
		{
			if(msg.isRegular())
			{
			
				String sender = msg.getSender().toString();
				String a[] = sender.split("#");
				sender = a[1];
				System.out.print("\nRecebida uma mensagem [REGULAR] ");
				System.out.println(" enviada por  " + sender + ".");
								
				//SpreadGroup groups[] = msg.getGroups();
				//System.out.println("para " + groups.length + " grupos.");
				
				byte data[] = msg.getData();
				String dado = new String(data);
				String newInfo = dado;
				//System.out.println("A informacao tem " + data.length + " bytes.");
				System.out.println("A mensagem eh: " + dado);
				String fileName;
				
				char letra = dado.charAt(0);
				//System.out.println("Letra = " + letra);
				int ID = 0;
				int PRIORITY = 0;
				
				String[] s = dado.split("#");
				
				if (letra == 'J' || letra == 'R') {
					ID = Integer.parseInt(s[1]);
					//System.out.println("ID = " + ID); 
					PRIORITY = Integer.parseInt(s[2]);
					//System.out.println("PRIORITY = " + PRIORITY);
				}
				
				String slave = "";
				int numReplicas = 1;
								
				// Envia a mensagem de retorno para os outros processos preencherem seus vetores de prioridades
				
				switch (letra){
				
				
					case 'J':
			
						// Monta a mensagem de resposta
						String info = new String();
						info = "R#" + id + "#" + Integer.toString(priority);
						
						sendMessage(info, groupName);
					
						break;
					
				
					case 'R': 
						// Adiciona o processo que respondeu Ã  lista de processos
						
						ServerProcess process = new ServerProcess(ID, PRIORITY);
						System.out.println("Adicionando o processo " + process + " na lista de processos.");
						
						addProcess(process);
						
						election();
						
		
						// Imprime a lista de processos
						System.out.print("\nLista de processos atual: \n\t");
						printProcessesList();
						
						/*for (ServerProcess p : this.processesList){
							System.out.print(p + " ");
						}
						*/
						
						//System.out.println("\nEu sou o master: " + this.isMaster);
						
						break;
						
					case 'S':
						
						// Monta a lista de slaves
						
						String novoSlave = s[1];
						slavesList.add(novoSlave);
					
						break;
					

					case 'C':
						// Processo Servidor Master trata a mensagem recebida do cliente
						// e envia a mensagem para criar o arquivo no grupo de processos Slave
						
						fileName = s[1];
						numReplicas = Integer.parseInt(s[2]);
						// Limita o nÃºmero de rÃ©plicas ao nÃºmero de Slaves (storages)
						if (numReplicas > slavesList.size())
							numReplicas = slavesList.size();
						
						boolean arquivoExiste = false;
						
						// Verifica se o arquivo jÃ¡ estÃ¡ na lista de arquivos criados
						for (String f : storedFilesList) {
							
							String fi[] = f.split("#");
							
							
							if (fi[0].equals(fileName)){
								// Arquivo solicitado para criaÃ§Ã£o jÃ¡ foi criado anteriormente
								
								String resp = "E#Arquivo jÃ¡ existe no ServiÃ§o de Arquivos#" + sender;
								System.out.println(resp);
								if (this.isMaster)
									sendMessage(resp, "CLIENT_GROUP");
								
								arquivoExiste = true;
								
								break;
							}
								
						}
						
						if (!arquivoExiste) {
							
							//sendMessage("E#Arquivo nÃ£o existe", "CLIENT_GROUP");

							for (int i = 0; i < numReplicas; i++){

								// Seleciona o Slave da vez que vai armazenar o arquivo - Balanceamento por rounding robin
								String currentSlave = getNextSlave();
								newInfo = dado + "#" + sender + "#" + currentSlave;
								String operation = newInfo;
								System.out.println("Slave da vez: " + currentSlave);

								// Insere a operaÃ§Ã£o no inÃ­cio da fila
								opsQueue.offer(operation);

								String currentOp;

								currentOp = opsQueue.peek();


								if (this.isMaster){


									System.out.println("Enviando a solicitaÃ§Ã£o para criar o arquivo: " + fileName + " no SLAVE_GROUP");

									System.out.println("Enviando mensagem: <" + currentOp + ">");

									sendMessage(currentOp, "SLAVE_GROUP");
									
									//inicia o timer com 5 segundos
									
									//aguardaResposta();
									
									//if (timeout) {
									// sendMessage("ServiÃ§o de Arquivos indisponÃ­vel", "CLIENT_GROUP");
									
									opsQueue.poll();

								}
							}
							if (this.isMaster)
								sendMessage("K#Arquivo criado com sucesso", "CLIENT_GROUP");

						} else
							if (this.isMaster)
								sendMessage("E#Arquivo jÃ¡ exite no ServiÃ§o de Arquivos", "CLIENT_GROUP");
						
						break;

					case 'K':
						
						// Processo Servidor Master trata a mensagem recebida do SLAVE_GROUP
						// e envia a mensagem para o cliente informando que o arquivo foi criado

						// Insere o registro do arquivo na lista de arquivos armazenados
						if (!s[1].equals("Arquivo deletado com sucesso.") && !s[1].equals("Arquivo editado com sucesso."))
						      storedFilesList.add(dado.substring(2));
						
						System.out.println("\nLista de arquivos armazenados: ");
						for (String f : storedFilesList)
							System.out.println(f);

						if (this.isMaster){
							
							System.out.println("Enviando a resposta para o CLIENT_GROUP");
							
							System.out.println("Enviando mensagem: <" + newInfo + ">");
							
							//sendMessage(newInfo, "CLIENT_GROUP");
							
						}

						// Remove a operaÃ§Ã£o do inÃ­cio da fila
						opsQueue.poll();

						
						break;
						
					case 'L': 
						// Mensagem para ler o arquivo



						fileName = s[1];

						System.out.println("Lendo o arquivo " + fileName);

						slave = "";

						if (this.isMaster){
							
							if (arquivoExiste(fileName)) {

								String slaves = getSlaves(fileName);
								
								slave = slaves.split("#")[1];
								
								// Insere a operaÃ§Ã£o no inÃ­cio da fila
								opsQueue.offer(dado);

								newInfo = dado + "#" + slave;

								System.out.println("Enviando a msg " + newInfo);
								sendMessage(newInfo, "SLAVE_GROUP");
							} else {

								sendMessage("E#Arquivo nÃ£o existe", "CLIENT_GROUP");

							}

						}


						break;
						
					case 'l':
						
						// Processo Servidor Master trata a mensagem recebida do SLAVE_GROUP
						// e envia a mensagem para o cliente com a leitura do arquivo

						if (this.isMaster){
							
							System.out.println("Enviando a resposta para o CLIENT_GROUP");
							
							System.out.println("Enviando mensagem: <" + newInfo + ">");
							
							sendMessage(newInfo, "CLIENT_GROUP");

							// Remove a operaÃ§Ã£o do inÃ­cio da fila
							opsQueue.remove();
							
						}


						
						break;
						
					case 'D': 
						// Mensagem para deletar o arquivo
						
						List<String> slavesComArquivosD = new ArrayList<>();

						fileName = s[1];

						System.out.println("Deletando o arquivo " + fileName);

						List<String> storedFilesListAux = new ArrayList<String>(storedFilesList);
						
						arquivoExiste = false;
						// Verifica se o arquivo jÃ¡ estÃ¡ na lista de arquivos criados
						for (String f : storedFilesListAux) {

							String fi[] = f.split("#");


							if (fi[0].equals(fileName)){
								// Arquivo que se quer deletar foi encontrado na lista de arquivos armazenados

								System.out.println("Arquivo " + fileName + " encontrado no slave " + fi[1]);
								slavesComArquivosD.add(fi[1]);

								arquivoExiste = true;

								storedFilesList.remove(f);
								System.out.println("Excluindo o arquivo " + fileName + " do slave " + fi[1]);
								System.out.println();
								

							}

						}

						if (arquivoExiste) {

							for (String slaveID : slavesComArquivosD) {
								// Insere a operaÃ§Ã£o no inÃ­cio da fila

								opsQueue.offer(dado);

								if (this.isMaster){

									newInfo = dado + "#" + slaveID;

									System.out.println("Enviando a msg " + newInfo);
									sendMessage(newInfo, "SLAVE_GROUP");
									
								} 
							}
							if (this.isMaster)
								sendMessage("K#Arquivo deletado com sucessso.", "CLIENT_GROUP");

						} else {

							System.out.println("Arquivo nÃ£o existe");
							if (this.isMaster)
								sendMessage("E#Arquivo nÃ£o existe", "CLIENT_GROUP");

						}

						break;
						
					case 'W': 
						// Mensagem para editar o arquivo
						
						List<String> slavesComArquivosW = new ArrayList<>();

						fileName = s[1];

						System.out.println("Enviando msg para editar o arquivo " + fileName);

						arquivoExiste = false;
						// Verifica se o arquivo jÃ¡ estÃ¡ na lista de arquivos criados
						for (String f : storedFilesList) {

							String fi[] = f.split("#");


							if (fi[0].equals(fileName)){
								// Arquivo que se quer deletar foi encontrado na lista de arquivos armazenados

								slavesComArquivosW.add(fi[1]);
								arquivoExiste = true;

							}

						}

						if (arquivoExiste) {
							
							if (this.isMaster)
								sendMessage("K#Arquivo editado com sucesso.", "CLIENT_GROUP");

							for (String slaveID : slavesComArquivosW) {
								// Insere a operaÃ§Ã£o no inÃ­cio da fila
								opsQueue.offer(dado);

								if (this.isMaster){

									newInfo = dado + "#" + slaveID;

									System.out.println("Enviando a msg " + newInfo);
									sendMessage(newInfo, "SLAVE_GROUP");

								} 
								
							}

							} else {
								if (this.isMaster)
									sendMessage("E#Arquivo nÃ£o existe", "CLIENT_GROUP");

							}
						

						break;
						
					default: 
						
						break;
				
				}
			}
			else if (msg.isMembership())
			{
				System.out.println("\nRecebida uma mensagem [DE GRUPO]: ");
				MembershipInfo info = msg.getMembershipInfo();
				handleMembershipInfo(info);
				
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
			System.exit(1);
		}
	}

	
	private boolean arquivoExiste(String fName) {
		
		// Verifica se o arquivo jÃ¡ estÃ¡ na lista de arquivos criados
		
		for (String f : storedFilesList) {
			
			String fi[] = f.split("#");
			
			if (fi[0].equals(fName)) {
				// Arquivo solicitado para criaÃ§Ã£o jÃ¡ foi criado anteriormente
				
				return true;
				
			}
				
		}

		return false;
	}

	private String getSlaves(String fName) {
		
		// Verifica se o arquivo jÃ¡ estÃ¡ na lista de arquivos criados
		
		String listaDeSlaves = fName;
		
		for (String f : storedFilesList) {
			
			String fi[] = f.split("#");
			
			if (fi[0].equals(fName)) {
								
				listaDeSlaves += "#" + fi[1];
				
			}
				
		}
		return listaDeSlaves;

	}

	
	private void sendMessage(String info, String groupName) {
		
		SpreadMessage msg = new SpreadMessage();
		msg.setSafe();
		msg.addGroup(groupName);

		msg.setData(info.getBytes());
		// Envia a mensagem
		try {
			connection.multicast(msg);
		} catch (SpreadException e) {
			e.printStackTrace();
		}

	}
	
	// Construtor
	public DServer(String user, int priority, String address, int port)
	{
		super(Integer.parseInt(user.substring(7)), priority);
		this.turnMaster(false);
		this.processesList = new ArrayList<ServerProcess>();
		this.opsQueue = new LinkedList<String>();
		this.storedFilesList = new ArrayList<String>();
		
		// Cria e inicializa a lista de processos servidores escravos (SLAVES)
		this.slavesList = new ArrayList<>();
		/*slavesList.add("SLAVE_1");
		slavesList.add("SLAVE_2");
		slavesList.add("SLAVE_3");
		slavesList.add("SLAVE_4");*/
		
		currentSlave = -1;
				
		startSpreadConnection(user, address, port);
	}
	
	public String getNextSlave() {
		
		currentSlave++;
		if (currentSlave == slavesList.size())
			currentSlave = 0;
		
		
		return slavesList.get(currentSlave);
	}

	
	public void addProcess(ServerProcess process) {
		
		boolean processIsInTheList = false;
		for (ServerProcess p : this.processesList){
			if (p.getId() == process.getId())
				processIsInTheList = true;
		}
		
		if (!processIsInTheList){
			System.out.println("O Processo " + process + " NAO esta na lista.");
			this.processesList.add(process);
		}
		else System.out.println("O Processo " + process + " JA esta na lista.");
		
	}

	public void removeProcess(ServerProcess process) {
		
		List<ServerProcess> list = this.processesList;
		
		for (int i = 0; i < list.size(); i ++){
			if (list.get(i).equals(process))
				list.remove(i);
		}

	}
	
	public void election() {
		
		List<ServerProcess> list = this.processesList;
		int lastIndex = list.size() - 1;
		
		Collections.sort(list);
		
		if (this.getId() == list.get(lastIndex).getId()){
			this.turnMaster(true);
		} else this.turnMaster(false);
		
	}
	
	public void printProcessesList() {

		List<ServerProcess> list = this.processesList;
		int lastIndex = list.size() - 1;

		for (ServerProcess p : this.processesList)
			System.out.print(p + " ");

		// Imprime o processo master
		System.out.println("\nMaster Process: " + list.get(lastIndex));
		System.out.println("___________________________________________________________________________");
		System.out.println();

	}
	
	public void turnMaster(boolean b) {
		this.isMaster = b;
	}
	
	public ServerProcess getMaster() {
		
		List<ServerProcess> list = this.processesList;
		int lastIndex = list.size() - 1;

		return list.get(lastIndex);
		
	}
	
	private void startSpreadConnection(String user, String address, int port) {
		
		SpreadMessage msg;
		SpreadMessage msgreceived = new SpreadMessage();
		
		// Establish the spread connection.
		///////////////////////////////////
		try
		{
			connection = new SpreadConnection();
			connection.connect(InetAddress.getByName(address), port, user, false, true);

			// Join to the group specified by constant groupName
			//this.groupName = "SERVER_GROUP";
			this.spreadGroup = new SpreadGroup();
			this.spreadGroup.join(connection, groupName);
			System.out.println("Juntou-se ao " + spreadGroup + ".");
			
			// Send message to spreadGroup with ID and priority
			msg = new SpreadMessage();
			msg.setSafe();
			msg.addGroup(groupName);
			
			// Monta a mensagem de join
			String info = new String();
			info = "J#" + user.substring(7) + "#" + Integer.toString(priority);
			msg.setData(info.getBytes());
			// Envia a mensagem
			connection.multicast(msg);
			//System.out.print("Enviada a mensagem: ");
			//System.out.println(new String(msg.getData()));
		
		}
		catch(SpreadException e)
		{
			System.err.println("Houve um erro ao tentar conectar ao daemon.");
			if (e.getMessage().equals("Connection attempt rejected=-6"))
				System.err.println("Processo ja participa do grupo " + groupName);
			//e.printStackTrace();
			System.exit(1);
		}
		catch(UnknownHostException e)
		{
			System.err.println("O daemon nao foi encontrado. Tente executar o daemon spread na pasta java." + address);
			System.exit(1);
		}
		
		// Server loop
		while(true){
		try{
			// Receive a message.
			/////////////////////
			msgreceived = connection.receive();
			messageReceived(msgreceived);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			System.exit(1);
		}
		}
				
	}
	
	private void closeSpreadConnection() {
        
		try {
			connection.disconnect();
		} catch (SpreadException e) {
			e.printStackTrace();
		}

	}

	public void messageReceived(SpreadMessage message)
	{
		resolveMessage(message);
	}

	
	
	public static final void main(String[] args)
	{		

		// Default values.
		//////////////////
		String user;
		String address = "127.0.0.1";
		int port = 0;
		int prio = 0;
		
		user = "SERVER_" + args[0];
		prio = Integer.parseInt(args[1]);		
		
		System.out.print("\nCriando o " + user + "\n");
		System.out.print(", com prioridade ");
		System.out.println(prio);
		

		new DServer(user, prio, address, port);

	}

}
