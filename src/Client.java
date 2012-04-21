import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Scanner;


public class Client {
	private String serverAddress;
	private Integer serverPort;
	private Boolean alive;
	private Boolean validationError;
	private Integer port;
	private String name;
	
	public Client() {
		Scanner input = new Scanner(System.in);
		this.alive = true;
		System.out.println("Welcome. Please refer to the README for usage.");
		while(alive){
			System.out.print("> ");
			String command = input.nextLine();
			if (command.trim().startsWith("Send")){
				String line;
				while (!(line = input.nextLine()).trim().equals("."))
					command = command + "\n" + line;
			}
			this.validationError = false;
			processCommand(command.trim());
		}
	}

	private void processCommand(String command) {
		command = command.replaceAll("\\s*\\,\\s*", ",");
		String[] parts = command.split("\\s+");
		if (parts[0].equals("Server")){
			//Server command
			if (parts.length != 3){
				System.out.println("ERROR: Wrong number of arguments");
				validationError = true;
			} else {
				validateIPAddress(parts[1], false);
				validatePort(parts[2]);
			}
			if (!validationError){
				if (this.port != null) //registered
					unregister("Unregister " + this.name, this.name);
				serverAddress = parts[1];
				serverPort = new Integer(parts[2]);
				System.out.println("OK - server set successfully to " + serverAddress + ":" + serverPort);
			}
		} else if (parts[0].equals("Test")){
			//Test command
			validateServerPresence();
			if (validationError)
				return;
			String test = sendAndReceive(command);
			if (test.startsWith("ERROR"))
				System.out.println(test);
			else
				System.out.println("Server alive and ready");
		} else if (parts[0].equals("Quit")){
			//Quit command
			this.alive = false;
			if (this.port != null) //registered
				unregister("Unregister " + this.name, this.name);
		} else if (parts[0].equals("Kill")){
			//Kill command
			validateServerPresence();
			System.out.println(sendAndReceive(command));
			if (!validationError){
				serverAddress = null;
				serverPort = null;
				if (port != null) //listener active
					teardownListener();
			}
		} else if (parts[0].equals("Find")){
			//Find command
			validateServerPresence();
			if (parts.length != 3){
				System.out.println("ERROR: Wrong number of arguments");
				validationError = true;
			} else {
				validateName(parts[1], true, false, false);
				validateIPAddress(parts[2], true);
			}
			if (!validationError){
				System.out.println(sendAndReceive(command));
			}
		} else if (parts[0].equals("Insert")){
			//Insert Command
			validateServerPresence();
			if (parts.length != 3){
				System.out.println("ERROR: Wrong number of arguments");
				validationError = true;
			} else {
				validateIPAddress(parts[1], false);
				validatePort(parts[2]);
			}
			if (!validationError){
				System.out.println(sendAndReceive(command));
			}
		} else if (parts[0].equals("Delete")){
			//Delete command
			validateServerPresence();
			if (parts.length < 1 || parts.length > 3){
				System.out.println("ERROR: Wrong number of arguments");
				validationError = true;
			} else {
				if (parts.length > 1)
					validateIPAddress(parts[1], true);
				if (parts.length > 2)
					validatePort(parts[2]);
			}
			if (!validationError){
				System.out.println(sendAndReceive(command));
			}
		} else if (parts[0].equals("Link")){
			//Link command
			validateServerPresence();
			if (parts.length == 2){
				validateName(parts[1], false, false, false);
			} else if (parts.length == 3){
				validateIPAddress(parts[1], false);
				validatePort(parts[2]);
			} else {
				System.out.println("ERROR: Wrong number of arguments");
				validationError = true;
			}
			if (!validationError){
				System.out.println(sendAndReceive(command));
			}
		} else if (parts[0].equals("Unlink")){
			//Unlink command
			validateServerPresence();
			if (parts.length == 2){
				validateName(parts[1], false, false, false);
			} else if (parts.length == 3){
				validateIPAddress(parts[1], false);
				validatePort(parts[2]);
			} else {
				System.out.println("ERROR: Wrong number of arguments");
				validationError = true;
			}
			if (!validationError){
				System.out.println(sendAndReceive(command));
			}
		} else if (parts[0].equals("Register")){
			//Register command
			validateServerPresence();
			if (parts.length != 3){
				System.out.println("ERROR: Wrong number of arguments");
				validationError = true;
			} else {
				validateName(parts[1], false, false, false);
				validatePort(parts[2]);
			}
			if (!validationError){
				register(command, parts[1], new Integer(parts[2]));
			}
		} else if (parts[0].equals("Unregister")){
			//Unregister command
			validateServerPresence();
			if (parts.length != 2){
				System.out.println("ERROR: Wrong number of arguments");
				validationError = true;
			} else {
				validateName(parts[1], false, false, false);
			}
			if (!validationError){
				unregister(command, parts[1]);
			}
		} else if (parts[0].equals("List")){
			//List command
			validateServerPresence();
			if (parts.length == 2){
				validateName(parts[1], true, true, false);
			} else if (parts.length == 3){
				validateName(parts[1], true, true, false);
				validateName(parts[2], true, true, true);
			} else {
				System.out.println("ERROR: Wrong number of arguments");
				validationError = true;
			}
			if (!validationError){
				System.out.println(sendAndReceive(command));
			}
		} else if (parts[0].equals("Send")){
			//Send command
			validateServerPresence();
			if (parts.length < 4){
				System.out.println("ERROR: Wrong number of arguments");
				validationError = true;
			} else {
				validateName(parts[1], true, true, false);
				validateName(parts[2], true, true, true);
				String[] lines = command.split("\n");
				if (lines.length < 2){
					System.out.println("ERROR: empty message discarded");
					validationError = true;
				}
			}
			if (!validationError){
				System.out.println(sendAndReceive(command));
			}
		} else {
			//Unknown command
			System.out.println("ERROR: unknown command");
		}
	}
	
	private void unregister(String command, String name){
		if (port == null){
			System.out.println("ERROR: You are not registered");
			return;
		}
		if (!name.equals(this.name)){
			System.out.println("ERROR: You are not registered under this name");
			return;
		}
		String response = sendAndReceive(command);
		teardownListener();
		System.out.println(response);
	}
	
	private void teardownListener(){
		DatagramSocket socket;
		try {
			socket = new DatagramSocket();
			socket.setSoTimeout(2000);
			DatagramPacket packet = new DatagramPacket("STOP".getBytes(), "STOP".getBytes().length, InetAddress.getByName("127.0.0.1"), port);
			socket.send(packet);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("WARNING: failed to terminate listening thread");
		}
		this.port = null;
		this.name = null;
	}

	private void register(String command, String name, Integer port) {
		if (this.port != null){
			System.out.println("ERROR: You are already registered - Your name is " + this.name);
			return;
		}
		String response = sendAndReceive(command);
		
		if (!response.trim().startsWith("ERROR")){
			this.port = port;
			this.name = name;
			receiveMessages(port);
		}
		
		System.out.println(response);
	}
	
	private void receiveMessages(final Integer port){
		new Thread(new Runnable() {
			public void run() {
				Boolean alive = true;
				try {
					DatagramSocket socket = new DatagramSocket(port);
					while (alive){
						byte[] buf = new byte[1024];
						DatagramPacket packet = new DatagramPacket(buf, buf.length);
						socket.receive(packet);
						String message = new String(packet.getData(), 0, packet.getLength());
						if (message.trim().startsWith("STOP"))
							alive = false;
						else{
							System.out.println("NEW MESSAGE:\n" + message);
							System.out.print("> ");
						}
						buf = "OK".getBytes();
						packet = new DatagramPacket(buf, buf.length, packet.getAddress(), packet.getPort());
						socket.send(packet);
					}
					socket.close();
				} catch (Exception e) {
					e.printStackTrace();
					System.out.println("Error - port number might be already used");
				}
			}
		}).start();
	}

	private String sendAndReceive(String command) {
		String result = "";
		try {
			InetAddress address = InetAddress.getByName(serverAddress);
			DatagramSocket socket = new DatagramSocket();
			socket.setSoTimeout(12000);
			//sending
			DatagramPacket packet = new DatagramPacket(command.getBytes(), command.getBytes().length, address, serverPort);
			socket.send(packet);
			//receiving
			byte[] buf = new byte[1024];
			packet = new DatagramPacket(buf, buf.length);
			socket.receive(packet);
			result = new String(packet.getData(), 0, packet.getLength());
		} catch (UnknownHostException e) {
			result = "ERROR: Host not found";
		} catch (SocketTimeoutException e) {
			result = "ERROR: Request timed out. Server apparently unavailable";
		} catch (SocketException e) {
			result = "ERROR reading from socket";
		} catch (IOException e) {
			result = "ERROR writing to socket";
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
	
	private void validateName(String name, boolean wildcardAllowed, boolean listAllowed, boolean selfAllowed){
		if (wildcardAllowed && "*".equals(name))
			return;
		if (!name.matches("\\w{1,80}") && !(listAllowed && name.matches("\"(\\w{1,80}\\,)*\\w{1,80}(\\w{1,80}\\,)*\""))){
			System.out.println("ERROR: Invalid name");
			validationError = true;
			return;
		}
		if (!selfAllowed && (name.toLowerCase().equals("self") || (listAllowed && name.toLowerCase().matches("\"(.+\\,)?self(\\,.+)?\"")))){
			System.out.println("ERROR: SELF is a reserved name");
			validationError = true;
		}
	}
	
}
