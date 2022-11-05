package utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileHandler {
	
	public static void writeToFile(String fileName, Map<String,String> map)
			throws IOException {
        FileWriter fw = new FileWriter(fileName,false);
        try (BufferedWriter bw = new BufferedWriter(fw)) {
        	map.forEach((k, v) -> {
        		try {
        			bw.write(k + " IN A " + v +"\n");
        		} catch (IOException e) {
        			e.printStackTrace();
        		}
			});
        }
    }
	
	public static void appendToFile(String fileName, String domain, String address)
			throws IOException {
        FileWriter fw = new FileWriter(fileName, true);
        try (BufferedWriter bw = new BufferedWriter(fw)) {
    		try {
    			bw.write(domain + " IN A " + address +"\n");
    		} catch (IOException e) {
    			e.printStackTrace();
    		}
        }
    }
	
	public static Map<String, String> readFromFile(String fileName) throws IOException {
		//index = 0 for domains, 1 for addresses
        File file = new File(fileName);
        Map<String, String> strs = new HashMap<>();
        List<String> lines = Files.readAllLines(file.toPath());
        for(String line: lines) {
            String[] tArray = line.split(" ");
            strs.put(tArray[0], tArray[3]);
        }
        return strs;
    }
}