import java.net.InetAddress;
import java.net.UnknownHostException;

import spread.BasicMessageListener;
import spread.SpreadConnection;
import spread.SpreadException;
import spread.SpreadGroup;
import spread.SpreadMessage;


public class AguardaMensagem implements Runnable, BasicMessageListener {

	SpreadConnection connection;

		public void setConnection(SpreadConnection c){
			connection = c;
		}
	
		@Override
		public void run() {
	        SpreadMessage msgreceived = new SpreadMessage();
	        
/*			String address = "127.0.0.1";
			int port = 0;
			String user = "Cliente01";
	        startSpreadConnection(user, address, port);
	*/        
	        boolean aguardando = true;
	        
	        while(aguardando){
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
		
		private void startSpreadConnection(String user, String address, int port) {
			
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
					
					
					char letra = mensagemRecebida.charAt(0);
					
					//if (letra != 'l')
						

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



}
