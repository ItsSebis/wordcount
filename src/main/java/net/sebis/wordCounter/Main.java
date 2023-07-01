package net.sebis.wordCounter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {

    public JSONObject data;

    public static void main(String[] args) throws IOException {
        new Main();
    }

    public Main() throws IOException {
        System.out.println("Hello and welcome!");

        // set all data var
        try {
            data = new JSONObject(readFile("data.json"));
        } catch (FileNotFoundException ex) {
            File dataJSON = new File("data.json");
            dataJSON.createNewFile();
            writeToFile("data.json", "{}", true);
            data = new JSONObject(readFile("data.json"));
        }
        // set hashes
        JSONArray hashes;
        if (data.isNull("words")) {
            hashes = new JSONArray();
        } else {
            hashes = data.getJSONArray("hashes");
        }

        // add new files
        try {
            // getting which files to search
            Set<String> newFiles = listDir("files");
            List<String> noTargets = new ArrayList<>();
            for (String file : newFiles) {
                String[] fileSplit = file.split("\\.");
                if (!fileSplit[fileSplit.length - 1].equals("log") ||
                        hashes.toList().contains(new BigInteger(1, MessageDigest.getInstance("MD5").digest(Files.readAllBytes(Paths.get(file)))).toString(16))) {
                    noTargets.add(file);
                }
            }
            for (String file : noTargets) {
                newFiles.remove(file);
            }

            // searching files
            if (newFiles.size() > 0) {
                // new files
                for (String file : newFiles) {
                    try {
                        System.out.println("Reading file: " + file);
                        analyseFile(file);
                        hashes.put(new BigInteger(1, MessageDigest.getInstance("MD5").digest(Files.readAllBytes(Paths.get(file)))).toString(16));
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
                data.put("hashes", hashes);
                writeToFile("data.json", data.toString(), false);
            } else {
                System.out.println("No new files");
            }
        } catch (NullPointerException ex) {
            new File("files").mkdirs();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        List<JSONObject> sortWords = new ArrayList<>();
        if (!data.isNull("words")) {
            Map<String, Object> words = data.getJSONObject("words").toMap();

            for (String w : words.keySet()) {
                JSONObject jWord = data.getJSONObject("words").getJSONObject(w);
                sortWords.add(new JSONObject().put("key", w).put("count", jWord.getInt("count")));
            }
        }
        boolean sorting = true;
        while (sorting) {
            sorting = false;
            for (int i = 0; i < sortWords.size(); i++) {
                if (i == sortWords.size()-1) {
                    break;
                }
                JSONObject word = sortWords.get(i);
                JSONObject nextWord = sortWords.get(i+1);
                if (word.getInt("count") < nextWord.getInt("count")) {
                    Collections.swap(sortWords, i, i+1);
                    sorting = true;
                }
            }
        }

        StringBuilder out = new StringBuilder();
        for (JSONObject word : sortWords) {
            out.append(word.getString("key")).append(": ").append(word.getInt("count")).append("\n");
        }
        writeToFile("result.txt", out.toString(), false);
        System.out.println("Wrote results in result.txt");
        System.out.println("Program finished it's work and goes to sleep. zzz...");
    }

    public Set<String> listDir(String dir) {
        return Stream.of(Objects.requireNonNull(new File(dir).listFiles())).filter(file -> !file.isDirectory()).map(File::getAbsolutePath).collect(Collectors.toSet());
    }

    public void analyseFile(String path) throws IOException {
        JSONObject words;
        if (data.isNull("words")) {
            words = new JSONObject("{}");
        } else {
            words = data.getJSONObject("words");
        }
        BufferedReader bR = new BufferedReader(new FileReader(path));
        String curLine;

        while ((curLine = bR.readLine()) != null) {
            // line content = curLine
            curLine = curLine.toLowerCase();
            curLine = curLine.replaceAll("[^\\p{Alpha}\\s+]", " ");
            String[] curWords = curLine.split("\\s+");
            for (String w : curWords) {
                if (w.equals("")) {
                    continue;
                }
                JSONObject jWord = new JSONObject();
                int count = 1;
                if (!words.isNull(w)) {
                    count += words.getJSONObject(w).getInt("count");
                }
                jWord.put("count", count);
                words.put(w, jWord);
                System.out.print(w + ": " + jWord + "\r");
            }
        }
        System.out.println();
        data.put("words", words);
        bR.close();
    }

    public String readFile(String path) throws IOException {
        StringBuilder content = new StringBuilder();
        BufferedReader bR = new BufferedReader(new FileReader(path));

        String curLine;
        while ((curLine = bR.readLine()) != null) {
            content.append(curLine);
        }
        bR.close();
        return content.toString();
    }

    public void writeToFile(String path, String content, boolean append) {
        try {
            BufferedWriter bW = new BufferedWriter(new FileWriter(path, append));
            if (append) {
                bW.append(content);
            } else {
                bW.write(content);
            }
            bW.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}