
import spread.*;

import java.net.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.io.*;


public class DSlave extends ServerProcess implements BasicMessageListener {
	
	private static String userName;

	private final String groupName = "SLAVE_GROUP";

	// The Processes List from each process 
	public List<ServerProcess> processesList;
	
	// Atribute that says if this process is the master process from spreadGroup
	private boolean isMaster;

	// The Spread Connection.
	private SpreadConnection connection;
	
	// A spreadGroup.
	private SpreadGroup spreadGroup;
	
	
	// Print this membership data.  Does so in a generic way so identical
	// function is used in recThread and User. 
	private void handleMembershipInfo(MembershipInfo info) {

		if(info.isRegularMembership()) {

			//System.out.print("\tDevido a ");
			if(info.isCausedByJoin()) {
				//System.out.println("entrada de " + info.getJoined());
			}	else if(info.isCausedByLeave()) {
				//System.out.println("saida de " + info.getLeft());
				// Tratar mensagem de um processo que foi cancelado
				
				
			}	else if(info.isCausedByDisconnect()) {
				
				// monta a mensagem pela desconexao
				//System.out.println("desconexao de " + info.getDisconnected());
				String dado = info.getDisconnected() + "";
				String[] s = dado.split("#");
				
				int IDx = Integer.parseInt(s[1].substring(6));
				//System.out.println("Remover o processo: " + IDx);
				ServerProcess p = new ServerProcess(IDx, 0);
				
				removeProcess(p);
				election();
				
				// Imprime a lista de processos
				//System.out.print("Lista de processos atual: \n\t");
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
			
				System.out.print("\nRecebida uma mensagem [REGULAR] ");
				System.out.println(" enviada por  " + msg.getSender() + ".");
								
				//SpreadGroup groups[] = msg.getGroups();
				//System.out.println("para " + groups.length + " grupos.");
				
				byte data[] = msg.getData();
				String dado = new String(data);
				//System.out.println("A informacao tem " + data.length + " bytes.");
				System.out.println("A mensagem eh: " + dado);
				
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
				
				String slaveName = "";
								
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
						//System.out.println("Adicionando o processo " + process + " na lista de processos.");
						
						addProcess(process);
						
						election();
						
						// Imprime a lista de processos
						//System.out.print("Lista de processos atual: \n\t");
						
						//for (ServerProcess p : this.processesList){
						//	System.out.print(p + " ");
						//}
						
						// Imprime a lista de processos
						//System.out.print("\nLista de processos atual: \n\t");
						//printProcessesList();
						
						break;
						
					case 'C':
						// Processo Servidor Master trata a mensagem recebida do cliente
						// e envia a mensagem para criar o arquivo no grupo de processos Slave
						String fileName = s[1];
						String receiverSlave = s[4];
						
						// Verifica se Ã© o Slave da Vez
						if (receiverSlave.equals(this.userName)) {
							System.out.println("Criando o arquivo: " + fileName + " na pasta " + userName);
							
							criarArquivo(fileName);
							
							// Envia a mensagem de retorno para o SERVER_GROUP informando que o arquivo foi criado
							
							info = "K#" + fileName + "#" + this.userName;
							sendMessage(info, "SERVER_GROUP");
							
						}
						
						break;
						
					case 'D':
						// Processo Servidor Master trata a mensagem recebida do cliente
						// e envia a mensagem para deletar o arquivo no grupo de processos Slave
						fileName = s[1];
						slaveName = s[2];
						
						if (slaveName.equals(this.userName)){
							System.out.println("Apagando o arquivo " + fileName);
							deletarArquivo(fileName);
							
							sendMessage("K#Arquivo deletado com sucesso.", "SERVER_GROUP");
						}
						
						break;
							
					case 'L':
						// Processo Servidor Master trata a mensagem recebida do cliente
						// e envia a mensagem para deletar o arquivo no grupo de processos Slave
						fileName = s[1];
						slaveName = s[2];
						
						if (slaveName.equals(this.userName)){
							System.out.println("Lendo o arquivo " + fileName);
							
							String leitura = this.lerArquivo(fileName);
							
							sendMessage("l#" + leitura, "SERVER_GROUP");
						}
						
						break;
							
					case 'W':
						// Processo Servidor Master trata a mensagem recebida do cliente
						// e envia a mensagem para editar o arquivo no grupo de processos Slave
						fileName = s[1];
						receiverSlave = s[3];
						
						// Verifica se Ã© o Slave da Vez
						if (receiverSlave.equals(this.userName)) {
							System.out.println("Editando o arquivo: " + fileName + " na pasta " + userName);
							
							escreverNoArquivo(fileName, s[2]);
							
							// Envia a mensagem de retorno para o SERVER_GROUP informando que o arquivo foi criado
							
							info = "K#Arquivo editado com sucesso.#";
							sendMessage(info, "SERVER_GROUP");
							
						}
						
						break;
						
					default: 
						
						break;
				
				}
			}
			else if (msg.isMembership())
			{
				//System.out.println("\nRecebida uma mensagem [DE GRUPO]: ");
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
	
	// OperaÃ§Ãµes para criar, escrever, ler e deletar arquivos
	

	public void criarArquivo(String fileName) {
		try {
			String path = "./src/" + userName + "/" + fileName;
			//System.out.println("Path: " + path);
			File arquivo = new File(path);
			if (!arquivo.exists()) {
				arquivo.createNewFile();
				System.out.println("Arquivo " + arquivo.getName() + " criado em " + arquivo.getCanonicalPath());
			} else {
				System.out.println("Arquivo " + arquivo.getName() + " jÃ¡ existe em " + arquivo.getParent());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void deletarArquivo(String fileName) {
		
			String path = "./src/" + userName + "/" + fileName;
		
			File arquivo = new File(path);
			if (!arquivo.exists()) {
				System.out.println("Arquivo " + arquivo.getName() + " nÃ£o existe em " + arquivo.getParent());
			} else {
				arquivo.delete();
				System.out.println("Arquivo " + arquivo.getName() + " deletado");
			}
	}

	
	public String lerArquivo(String fileName) {
		
		String texto = "";
		
		try {
			
			String path = "./src/" + userName + "/" + fileName;

			File arquivo = new File(path);
			
			FileReader ler = new FileReader(arquivo);
			BufferedReader lerBuffer = new BufferedReader(ler);
			
			String linha = lerBuffer.readLine();
			
			
			
			while (linha != null) {
				//System.out.println(linha);
				texto = texto.concat(linha + "\n");
				linha = lerBuffer.readLine();
			}
			
			return texto;
			
		} catch (IOException e) {
			e.printStackTrace();
			return "";
		}

	}

	public void escreverNoArquivo(String fileName, String texto) {
		
		String path = "./src/" + userName + "/" + fileName;
		
		File arquivo = new File(path);

		try {
			if (!arquivo.exists()) {
				System.out.println("Arquivo " + arquivo.getName() + " nÃ£o existe em " + arquivo.getParent());
			} else {
			
				FileWriter fileWriter = new FileWriter(arquivo, true);
				BufferedWriter bufferWriter = new BufferedWriter(fileWriter);
				
				bufferWriter.write(texto);
				bufferWriter.newLine();
				
				bufferWriter.close();
				fileWriter.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	
	public DSlave(String user, int priority, String address, int port)
	{
		super(Integer.parseInt(user.substring(6)), priority);
		this.turnMaster(false);
		this.processesList = new ArrayList<ServerProcess>();
				
		
		// Cria o diretÃ³rio para armazenar os arquivos
		String dir = "./src/" + this.userName;
		File novoDiretorio = new File(dir);
		boolean criou = novoDiretorio.mkdirs(); //cria o diretÃ³rio
		
		System.out.println("Criou o diretÃ³rio " + dir + ": " + criou);
		
		startSpreadConnection(user, address, port);
		
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
		//System.out.println("\nMaster Process: " + list.get(lastIndex));
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
			//this.groupName = "SLAVE_GROUP";
			this.spreadGroup = new SpreadGroup();
			this.spreadGroup.join(connection, groupName);
			System.out.println("Juntou-se ao " + spreadGroup + ".");
			
			// Envia a mensagem para o SERVER_GROUP adicionar na lista de slaves
			sendMessage(("S#" + this.userName), "SERVER_GROUP");


			
			// Send message to spreadGroup with ID and priority
			msg = new SpreadMessage();
			msg.setSafe();
			msg.addGroup(groupName);
			
			// Monta a mensagem de join
			String info = new String();
			info = "J#" + user.substring(6) + "#" + Integer.toString(priority);
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
		int prio = 1;
		
		user = "SLAVE_" + args[0];
		userName = user;
		if (args.length == 2)
			prio = Integer.parseInt(args[1]);
		
		System.out.print("\nCriando o " + user + "\n");
		//System.out.print(", com prioridade ");
		//System.out.println(prio);
		

		new DSlave(user, prio, address, port);
				
	}

}
