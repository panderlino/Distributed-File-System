// Implementar um cliente que possa criar, excluir, ler e escrever um arquivo no serviÃ§o de arquivos

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Scanner;

import spread.*;

public class Client implements BasicMessageListener {

	static Thread escutadorDeMensagens;
	
	public static String fileName;
	
	// The Spread Connection.
	private static SpreadConnection connection;
	
	private static String userName;
	
    boolean aguardando;


	public Client(String user, String address, int port) {
		userName = user;
		
        //startSpreadConnection(userName, address, port);

        int opcao;
        Scanner entrada = new Scanner(System.in);
        
		escutadorDeMensagens.start();

        
        do{
            menu();
            opcao = entrada.nextInt();
            
            switch(opcao){
            case 1:
                System.out.print("Digite o nome do arquivo que deseja criar: ");
               
                readFileName();
                
                System.out.print("Digite a quantidade de rÃ©plicas que deseja criar: ");
                String numReplicas = ""; 
        		try {
                    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
                    numReplicas = in.readLine();
                } catch (IOException exception) {
                    exception.printStackTrace();
                }


                // Monta a mensagem de criaÃ§Ã£o de arquivo
                String info = new String();
				info = "C#" + fileName + "#" + numReplicas;
                sendMessage(info, "SERVER_GROUP");
				
                System.out.println("Enviando mensagem <" + info +  "> para o SERVER_GROUP para a criaÃ§Ã£o do arquivo " + fileName);
                
            	// Aguarda a mensagem de resposta do servidor
               // aguardaResposta();
                                
                break;
                
            case 2:
                System.out.println("Chamada do mÃ©todo para escrever em um arquivo");
                //escrever arquivo
                
                System.out.print("Digite o nome do arquivo que deseja editar: ");
                
                readFileName();
                System.out.print("Escreva o texto do arquivo e aperte enter\n>");
                
                String texto = "";
        		try {
                    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
                    texto = in.readLine();
                } catch (IOException exception) {
                    exception.printStackTrace();
                }
                
                
                // Monta a mensagem de ediÃ§Ã£o de arquivo
				info = "W#" + fileName + "#" + texto;
                sendMessage(info, "SERVER_GROUP");
				
                System.out.println("Enviando mensagem <" + info +  "> para o SERVER_GROUP para a ediÃ§Ã£o do arquivo " + fileName);
                
            	// Aguarda a mensagem de resposta do servidor
               // aguardaResposta();
                
                break;
                
            case 3:
                System.out.println("Chamada do mÃ©todo para ler um arquivo");
                System.out.print("Digite o nome do arquivo que deseja ler do ServiÃ§o de Arquivos: ");
                
                readFileName();
                
                // Monta a mensagem de leitura do arquivo
                info = new String();
				info = "L#" + fileName;
                sendMessage(info, "SERVER_GROUP");
				
                System.out.println("Enviando mensagem <" + info +  "> para o SERVER_GROUP para a leitura do arquivo " + fileName);

                //aguardaResposta();

                break;
                
            case 4:
                System.out.println("Chamada do mÃ©todo para excluir  um arquivo");
                
                System.out.print("Digite o nome do arquivo que deseja excluir : ");
                
                readFileName();
                
                // Monta a mensagem de criaÃ§Ã£o de arquivo
                info = new String();
				info = "D#" + fileName;
                sendMessage(info, "SERVER_GROUP");
				
                System.out.println("Enviando mensagem <" + info +  "> para o SERVER_GROUP para a deleÃ§Ã£o do arquivo " + fileName);
                
            	// Aguarda a mensagem de resposta do servidor
                //aguardaResposta();



                
                break;
            
            case 9:
                System.out.println("Saindo");
                //consulta();
                break;
            
            default:
                System.out.println("OpÃ§Ã£o invÃ¡lida.");
            }

        } while(opcao != 9);
        
	}

	public void menu(){
        System.out.println("\n\tCliente do ServiÃ§o de Arquivos DistribuÃ­do\n");
        System.out.println("1. Criar arquivo");
        System.out.println("2. Escrever arquivo");
        System.out.println("3. Ler arquivo");
        System.out.println("4. Excluir arquivo");
        System.out.println("9. Sair\n");
        
        //System.out.println("\nOpcao:");
    }	
	
	private static void closeSpreadConnection() {
        
		try {
			connection.disconnect();
		} catch (SpreadException e) {
			e.printStackTrace();
		}

	}

	private void readFileName() {
        
		try {
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            fileName = in.readLine();
        } catch (IOException exception) {
            exception.printStackTrace();
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
	
	private static void startSpreadConnection(String user, String address, int port) {
		
		// Establish the spread connection.
		///////////////////////////////////
		try
		{
			connection = new SpreadConnection();
			connection.connect(InetAddress.getByName(address), port, user, false, true);
			
			// Join the group CLIENT_GROUP
			SpreadGroup spreadGroup = new SpreadGroup();
			spreadGroup.join(connection, "CLIENT_GROUP");
			System.out.println("Juntou-se ao " + spreadGroup + ".");
			
			System.out.println("Aberta a conexÃ£o de " + user + " com " + address + ":" + port);

		}
		catch(SpreadException e)
		{
			System.err.println("Houve um erro ao tentar conectar ao daemon.");
			e.printStackTrace();
			System.exit(1);
		}
		catch(UnknownHostException e)
		{
			System.err.println("O daemon nao foi encontrado. Tente executar o daemon spread na pasta java." + address);
			System.exit(1);
		}
		
	}

	
	
	@Override
	public void messageReceived(SpreadMessage msg) {
		
		try{
			if (msg.isRegular()){
				//System.out.print("\nRecebida uma mensagem [REGULAR] ");
				//System.out.println(" enviada por  " + msg.getSender() + ".");
				
				byte data[] = msg.getData();
				String mensagemRecebida = new String(data);	
			
				//System.out.println("Recebida a mensagem <" + dado + ">");
				aguardando = false;
				
				char letra = mensagemRecebida.charAt(0);
				
				//if (letra != 'l')
					//aguardaResposta();

				String[] splitedMessage = mensagemRecebida.split("#");

				switch(letra) {

				case 'l':

					if (splitedMessage.length == 1)
						System.out.println("Arquivo Vazio");
					else {
						String texto = splitedMessage[1];
						System.out.println("Este Ã© o arquivo solicitado:\n");
						System.out.println("-------\n");
						System.out.println(texto);
						System.out.println("-------\n");
					}

					break;
					
				case 'E':
					
					String erro = splitedMessage[1];
					
					System.out.println(erro);
					
					
					break;

				case 'K':
					
					String okMsg = splitedMessage[1];
					
					System.out.println(okMsg);
					
					
					break;

				}

			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
			System.exit(1);
		}
		
	}

	public static void main(String[] args) {
		        
        fileName = "";

        String user = "cliente01";
		String address = "127.0.0.1";
		int port = 0;
		
		startSpreadConnection(user, address, port);
		
		AguardaMensagem wait = new AguardaMensagem();
		wait.setConnection(connection);
		
		escutadorDeMensagens = new Thread(wait);
		
		new Client(user, address, port);

        closeSpreadConnection();
	
	}

	
}
