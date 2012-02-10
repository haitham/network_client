import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Scanner;


public class Client {
	private String serverAddress;
	private Integer serverPort;
	Boolean alive;
	Boolean validationError;
	
	public Client() {
		Scanner input = new Scanner(System.in);
		this.alive = true;
		System.out.println("Welcome. Please refer to the README for usage.");
		while(alive){
			System.out.print("> ");
			String command = input.nextLine();
			this.validationError = false;
			processCommand(command);
		}
	}

	private void processCommand(String command) {
		if (command.startsWith("Server ")){
			String[] parts = command.split("\\s");
			if (parts.length != 3){
				System.out.println("ERROR: Wrong number of arguments");
				validationError = true;
			} else {
				validateIPAddress(parts[1], false);
				validatePort(parts[2]);
			}
			if (!validationError){
				serverAddress = parts[1];
				serverPort = new Integer(parts[2]);
				System.out.println("OK - server set successfully");
			}
		} else if (command.startsWith("Quit")){
			this.alive = false;
		} else if (command.startsWith("Kill")){
			validateServerPresence();
			if (!validationError){
				serverAddress = null;
				serverPort = null;
			}
		} else if (command.startsWith("Find ")){
			validateServerPresence();
			String[] parts = command.split("\\s");
			if (parts.length != 3){
				System.out.println("ERROR: Wrong number of arguments");
				validationError = true;
			} else {
				validateName(parts[1], true);
				validateIPAddress(parts[2], true);
			}
			if (!validationError){
				sendAndReceive(command);
			}
		} else if (command.startsWith("Insert ")){
			validateServerPresence();
			String[] parts = command.split("\\s");
			if (parts.length != 4){
				System.out.println("ERROR: Wrong number of arguments");
				validationError = true;
			} else {
				validateName(parts[1], false);
				validateIPAddress(parts[2], false);
				validatePort(parts[3]);
			}
			if (!validationError){
				sendAndReceive(command);
			}
		} else if (command.startsWith("Delete ")){
			validateServerPresence();
			String[] parts = command.split("\\s");
			if (parts.length < 2 || parts.length > 4){
				System.out.println("ERROR: Wrong number of arguments");
				validationError = true;
			} else {
				validateName(parts[1], true);
				if (parts.length > 2)
					validateIPAddress(parts[2], true);
				if (parts.length > 3)
					validatePort(parts[3]);
			}
			if (!validationError){
				sendAndReceive(command);
			}
		} else {
			System.out.println("ERROR: unknown command");
		}
	}

	private void sendAndReceive(String command) {
		try {
			InetAddress address = InetAddress.getByName(serverAddress);
			DatagramSocket socket = new DatagramSocket();
			//sending
			DatagramPacket packet = new DatagramPacket(command.getBytes(), command.getBytes().length, address, serverPort);
			socket.send(packet);
			//receiving
			byte[] buf = new byte[256];
			packet = new DatagramPacket(buf, buf.length);
			socket.receive(packet);
			System.out.println(new String(packet.getData(), 0, packet.getLength()));
		} catch (UnknownHostException e) {
			System.out.println("ERROR: Host not found");
		} catch (SocketException e) {
			System.out.println("Error initiating socket");
		} catch (IOException e) {
			System.out.println("Error writing to socket");
		}		
	}

	private void validateServerPresence() {
		if (serverAddress == null || serverPort == null){
			System.out.println("ERROR: You have to set the server address an port first. Use a \"Server\" command");
			validationError = true;
		}
	}

	private void validatePort(String port) {
		try {
			Integer portNumber = Integer.parseInt(port);
			if (portNumber < 1024 || portNumber > 65535){
				System.out.println("ERROR: Invalid port number. Should be an integer between 1024 and 65535");
				validationError = true;
			}
		} catch (NumberFormatException e) {
			System.out.println("ERROR: Invalid port number");
			validationError = true;
		}
	}

	private void validateIPAddress(String address, boolean wildcard) {
		String[] parts = address.split("\\.");
		if (parts.length != 4){
			System.out.println("ERROR: Invalid IP address");
			validationError = true;
		} else {
			for (int i=0; i<4; i++){
				if (wildcard && "*".equals(parts[i]))
					continue;
				try {
					Integer part = Integer.parseInt(parts[i]);
					if (part < 0 || part > 255){
						System.out.println("ERROR: Invalid IP address");
						validationError = true;
					}
				} catch (NumberFormatException e) {
					System.out.println("ERROR: Invalid IP address");
					validationError = true;
				}
			}
		}
	}
	
	private void validateName(String name, boolean wildcard){
		if (wildcard && "*".equals(name))
			return;
		if (!name.matches("\\w{1,80}")){
			System.out.println("ERROR: Invalid name");
			validationError = true;
		}
	}
	
}
