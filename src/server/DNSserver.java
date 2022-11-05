package server;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import utils.FileHandler;

public class DNSserver {
	private final static int PORT = 53;

	public static void main(String[] args){
		boolean running = true; //mientras corre el servidor
		byte[] requestBytes = new byte[1024];//donde se almacenan los bytes recibidos
		byte[] responseBytes = new byte[1024];//donde se almacenan los bytes de la respuesta

		String file = "src/masterFile/Hosts.txt";
		
		Map<String, String> addresses = new HashMap<String, String>();
		
		try {
			addresses = FileHandler.readFromFile(file);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		System.out.println("Servidor DNS iniciado");
		try {
			DatagramSocket socket = new DatagramSocket(PORT);
			
			while(running) {
				DatagramPacket packet = new DatagramPacket(requestBytes, requestBytes.length);
				
				socket.receive(packet);
				
				System.out.println("\nSe recibieron: " + packet.getLength() + " bytes");
				String domain = getDomain(requestBytes);
				
				if(domain != null) {
					
					String resolvedAddress = addresses.get(domain);
					
					if (resolvedAddress == null) {
						try {
							InetAddress address_search = java.net.InetAddress.getByName(domain); 
							resolvedAddress = address_search.getHostAddress();
							
							addresses.put(domain, resolvedAddress);
							
							FileHandler.appendToFile(file, domain, resolvedAddress);
						}
						catch(UnknownHostException ex) {}
					}
					if (resolvedAddress!=null) {
						responseBytes = createResponse(resolvedAddress, requestBytes);
						readResponse(responseBytes);
					}else {
						System.out.println("\nPeticion del cliente: " + domain + " | IP address: Sin resolucion");
					}
					
					DatagramPacket send_packet = new DatagramPacket(
							responseBytes, responseBytes.length, packet.getAddress(), packet.getPort());
					socket.send(send_packet);
				}else {
					System.out.println("\nTipo de solicitud no soportada");
				}
				
			}
			FileHandler.writeToFile(file, addresses);
			socket.close();
			
		} catch (SocketException e) {
			System.out.println("No se pudo conectar al puerto");
		} catch (IOException e) {
			System.out.println("Error en la recepcion del mensaje");
			e.printStackTrace();
		}
	}
	
	public static String getDomain(byte[] request) throws IOException {
		DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream(request));
        System.out.println("\n________Petcion recibida:________");
        System.out.println("-----Header-----");
        System.out.println("ID: " + dataInputStream.readShort()); // ID
        short flags = dataInputStream.readByte();
        int QR = (flags & 0b10000000) >>> 7;
        int opCode = (flags & 0b01111000) >>> 3;
        int AA = (flags & 0b00000100) >>> 2;
        int TC = (flags & 0b00000010) >>> 1;
        int RD = flags & 0b00000001;
        System.out.print("Flags: QR ("+QR);
        System.out.print("), Opcode ("+opCode);
        System.out.print("), AA ("+AA);
        System.out.print("), TC ("+TC);
        System.out.print("), RD ("+RD);
        flags = dataInputStream.readByte();
        int RA = (flags & 0b10000000) >>> 7;
        int Z = ( flags & 0b01110000) >>> 4;
        int RCODE = flags & 0b00001111;
        System.out.print("), RA ("+RA);
        System.out.print("), Z ("+ Z);
        System.out.print("), RCODE ("+RCODE+")");

        short QDCOUNT = dataInputStream.readShort();
        short ANCOUNT = dataInputStream.readShort();
        short NSCOUNT = dataInputStream.readShort();
        short ARCOUNT = dataInputStream.readShort();

        System.out.println("\nQuestions: " + String.format("%s", QDCOUNT));
        System.out.println("Answers RRs: " + String.format("%s", ANCOUNT));
        System.out.println("Authority RRs: " + String.format("%s", NSCOUNT));
        System.out.println("Additional RRs: " + String.format("%s", ARCOUNT));

        String QNAME = "";
        int recLen;
        while ((recLen = dataInputStream.readByte()) > 0) {
            byte[] record = new byte[recLen];
            for (int i = 0; i < recLen; i++) {
                record[i] = dataInputStream.readByte();
            }
            QNAME += "." +new String(record, StandardCharsets.UTF_8);
        }
        QNAME = QNAME.substring(1);
        short QTYPE = dataInputStream.readShort();
        short QCLASS = dataInputStream.readShort();
        System.out.println("\n-----Question-----");
        System.out.println("Question Name: " + QNAME);
        System.out.println("Question Type: " + String.format("%s", QTYPE));
        System.out.println("Question Class: " + String.format("%s", QCLASS));
        
        System.out.println("________________________________");
        
        if(QTYPE == 1) {        	
        	return QNAME;
        }else {
        	return null;
        }
	}
	
	public static byte[] createResponse(String address, byte[] requestBytes) throws IOException {
		//flujo de entrada de datos (para leer los bytes del packete de llegada)
		DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream(requestBytes));
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		//flujo de salida de datos (para llenar los bytes del packete de salida)
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
        
        dataOutputStream.writeShort(dataInputStream.readShort());//leyendo ID y escribiendolo en el otro mensaje
        short flags = dataInputStream.readShort();
        byte[] flagsBytes = new byte[2];
        flagsBytes[0] = (byte)(flags & 0xff);
        flagsBytes[1] = (byte)((flags << 8) & 0xff); 
        flagsBytes[0] = (byte)(flagsBytes[0] | (1 << 7));
        flagsBytes[0] = (byte)(flagsBytes[0] | (1 << 0));
        flagsBytes[1] = (byte)(flagsBytes[1] | (1 << 7));
        
        dataOutputStream.write(flagsBytes);
        dataOutputStream.writeShort(dataInputStream.readShort());//QDCOUNT
        dataOutputStream.writeShort(dataInputStream.readShort()+1);//ANCOUNT
        dataOutputStream.writeShort(dataInputStream.readShort());//NSCOUNT
        dataOutputStream.writeShort(dataInputStream.readShort());//ARCOUNT
        
        
        String QNAME = "";
        int recLen;
        while ((recLen = dataInputStream.readByte()) > 0) {
            byte[] record = new byte[recLen];
            for (int i = 0; i < recLen; i++) {
                record[i] = dataInputStream.readByte();
            }
            QNAME += "." +new String(record, StandardCharsets.UTF_8);
        }
        QNAME = QNAME.substring(1);
        String[] domainParts = QNAME.split("\\.");

        for (int i = 0; i < domainParts.length; i++) {
            byte[] domainBytes = domainParts[i].getBytes(StandardCharsets.UTF_8);
            dataOutputStream.writeByte(domainBytes.length);
            dataOutputStream.write(domainBytes);
        }
        
        dataOutputStream.writeByte(0);//fin domain
        short QTYPE = dataInputStream.readShort();
        short QCLASS = dataInputStream.readShort();
        dataOutputStream.writeShort(QTYPE);//QTYPE
        dataOutputStream.writeShort(QCLASS);//QCLASS
        
        //response
        byte[] resName = new byte[2];
        resName[0] = (byte) 0b11000000;
        resName[1] = (byte) 0b00001100;
        dataOutputStream.write(resName);//Response NAME
        dataOutputStream.writeShort(QTYPE);//Response TYPE (el mismo QTYPE)
        dataOutputStream.writeShort(QCLASS);//Response CLASS (el mismo QCLASS)
        byte[] TTL = new byte[4];
        TTL[0] = (byte) 0b00000000;
        TTL[1] = (byte) 0b00000000;
        TTL[2] = (byte) 0b00000001;
        TTL[3] = (byte) 0b00101100;
        dataOutputStream.write(TTL);//Response TTL
        byte[] dataLen = new byte[2];
        dataLen[0] = (byte) 0b00000000;
        dataLen[1] = (byte) 0b00000100;
        dataOutputStream.write(dataLen);//Response data length
        String[] addressSplit = address.split("\\.");
        byte[] addressBytes = new byte[4];
        for (int i=0; i<4; i++) {
        	addressBytes[i] = (byte) Integer.parseInt(addressSplit[i]);
        	dataOutputStream.writeByte(addressBytes[i]);
        }
        dataOutputStream.writeByte(0);
        dataOutputStream.close();
        dataInputStream.close();
		return byteArrayOutputStream.toByteArray();
	}
	
	public static void readResponse(byte[] response) throws IOException {
        DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream(response));
        System.out.println("\n________Respuesta enviada:________");
        System.out.println("-----Header-----");
        System.out.println("ID: " + dataInputStream.readShort()); // ID
        short flags = dataInputStream.readByte();
        int QR = (flags & 0b10000000) >>> 7;
        int opCode = (flags & 0b01111000) >>> 3;
        int AA = (flags & 0b00000100) >>> 2;
        int TC = (flags & 0b00000010) >>> 1;
        int RD = flags & 0b00000001;
        System.out.print("Flags: QR ("+QR);
        System.out.print("), Opcode ("+opCode);
        System.out.print("), AA ("+AA);
        System.out.print("), TC ("+TC);
        System.out.print("), RD ("+RD);
        flags = dataInputStream.readByte();
        int RA = (flags & 0b10000000) >>> 7;
        int Z = ( flags & 0b01110000) >>> 4;
        int RCODE = flags & 0b00001111;
        System.out.print("), RA ("+RA);
        System.out.print("), Z ("+ Z);
        System.out.print("), RCODE ("+RCODE+")");

        short QDCOUNT = dataInputStream.readShort();
        short ANCOUNT = dataInputStream.readShort();
        short NSCOUNT = dataInputStream.readShort();
        short ARCOUNT = dataInputStream.readShort();

        System.out.println("\nQuestions: " + String.format("%s", QDCOUNT));
        System.out.println("Answers RRs: " + String.format("%s", ANCOUNT));
        System.out.println("Authority RRs: " + String.format("%s", NSCOUNT));
        System.out.println("Additional RRs: " + String.format("%s", ARCOUNT));

        String QNAME = "";
        int recLen;
        while ((recLen = dataInputStream.readByte()) > 0) {
            byte[] record = new byte[recLen];
            for (int i = 0; i < recLen; i++) {
                record[i] = dataInputStream.readByte();
            }
            QNAME += "." +new String(record, StandardCharsets.UTF_8);
        }
        QNAME = QNAME.substring(1);
        short QTYPE = dataInputStream.readShort();
        short QCLASS = dataInputStream.readShort();
        System.out.println("\n-----Question-----");
        System.out.println("Question Name: " + QNAME);
        System.out.println("Question Type: " + String.format("%s", QTYPE));
        System.out.println("Question Class: " + String.format("%s", QCLASS));

        byte firstBytes = dataInputStream.readByte();
        int firstTwoBits = (firstBytes & 0b11000000) >>> 6;

        ByteArrayOutputStream label = new ByteArrayOutputStream();
        Map<String, String> domainToIp = new HashMap<>();
        
        System.out.println("\n-----Answers-----");
        for(int i = 0; i < ANCOUNT; i++) {
            if(firstTwoBits == 3) {
                byte currentByte = dataInputStream.readByte();
                boolean stop = false;
                byte[] newArray = Arrays.copyOfRange(response, currentByte, response.length);
                DataInputStream sectionDataInputStream = new DataInputStream(new ByteArrayInputStream(newArray));
                ArrayList<Integer> RDATA = new ArrayList<>();
                ArrayList<String> DOMAINS = new ArrayList<>();
                while(!stop) {
                    byte nextByte = sectionDataInputStream.readByte();
                    if(nextByte != 0) {
                        byte[] currentLabel = new byte[nextByte];
                        for(int j = 0; j < nextByte; j++) {
                            currentLabel[j] = sectionDataInputStream.readByte();
                        }
                        label.write(currentLabel);
                    } else {
                        stop = true;
                        short TYPE = dataInputStream.readShort();
                        short CLASS = dataInputStream.readShort();
                        int TTL = dataInputStream.readInt();
                        int RDLENGTH = dataInputStream.readShort();
                        for(int s = 0; s < RDLENGTH; s++) {
                            int nx = dataInputStream.readByte() & 255;// and with 255 to
                            RDATA.add(nx);
                        }

                        System.out.println("Type: " + TYPE);
                        System.out.println("Class: " + CLASS);
                        System.out.println("Time to live: " + TTL);
                        System.out.println("Record data Length: " + RDLENGTH);
                    }

                    DOMAINS.add(label.toString(StandardCharsets.UTF_8));
                    label.reset();
                }

                StringBuilder ip = new StringBuilder();
                StringBuilder domainSb = new StringBuilder();
                for(Integer ipPart:RDATA) {
                    ip.append(ipPart).append(".");
                }

                for(String domainPart:DOMAINS) {
                    if(!domainPart.equals("")) {
                        domainSb.append(domainPart).append(".");
                    }
                }
                String domainFinal = domainSb.toString();
                String ipFinal = ip.toString();
                domainToIp.put(ipFinal.substring(0, ipFinal.length()-1), domainFinal.substring(0, domainFinal.length()-1));

            }else if(firstTwoBits == 0){
                System.out.println("Es una repuesta tipo label");
            }

            firstBytes = dataInputStream.readByte();
            firstTwoBits = (firstBytes & 0b11000000) >>> 6;
        }

        domainToIp.forEach((key, value) -> System.out.println(key + " : " + value));
        System.out.println("________________________________");
	}
}
