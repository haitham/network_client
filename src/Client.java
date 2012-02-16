import java.io.IOException;
import java.io.PrintStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


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
			processCommand(command.trim());
		}
	}

	private void processCommand(String command) {
		String[] parts = command.split("\\s+");
		if (parts[0].equals("Server")){
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
		} else if (parts[0].equals("Test")){
			String test = sendAndReceive(command);
			if (test != null && !test.isEmpty())
				System.out.println("Server alive and ready");
		} else if (parts[0].equals("Quit")){
			this.alive = false;
		} else if (parts[0].equals("Kill")){
			validateServerPresence();
			System.out.println(sendAndReceive(command));
			if (!validationError){
				serverAddress = null;
				serverPort = null;
			}
		} else if (parts[0].equals("Find")){
			validateServerPresence();
			if (parts.length != 3){
				System.out.println("ERROR: Wrong number of arguments");
				validationError = true;
			} else {
				validateName(parts[1], true);
				validateIPAddress(parts[2], true);
			}
			if (!validationError){
				System.out.println(sendAndReceive(command));
			}
		} else if (parts[0].equals("Insert")){
			validateServerPresence();
			if (parts.length != 4){
				System.out.println("ERROR: Wrong number of arguments");
				validationError = true;
			} else {
				validateName(parts[1], false);
				validateIPAddress(parts[2], false);
				validatePort(parts[3]);
			}
			if (!validationError){
				System.out.println(sendAndReceive(command));
			}
		} else if (parts[0].equals("Delete")){
			validateServerPresence();
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
				System.out.println(sendAndReceive(command));
			}
		} else {
			System.out.println("ERROR: unknown command");
		}
	}

	private String sendAndReceive(final String command) {
		String result = "";
		try {
			InetAddress address = InetAddress.getByName(serverAddress);
			DatagramSocket socket = new DatagramSocket();
			socket.setSoTimeout(6000);
			//sending
			DatagramPacket packet = new DatagramPacket(command.getBytes(), command.getBytes().length, address, serverPort);
			socket.send(packet);
			//receiving
			byte[] buf = new byte[256];
			packet = new DatagramPacket(buf, buf.length);
			socket.receive(packet);
			result = new String(packet.getData(), 0, packet.getLength());
		} catch (UnknownHostException e) {
			System.out.println("ERROR: Host not found");
		} catch (SocketTimeoutException e) {
			System.out.println("ERROR: Request timed out. Server apparently unavailable");
		} catch (SocketException e) {
			System.out.println("Error reading from socket");
		} catch (IOException e) {
			System.out.println("Error writing to socket");
        }
		return result;
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
