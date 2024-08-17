import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    static List<TestData> testDataArray = null;

    public static void main(String[] args) {
        boolean excStatus = runCommandLoop();

        while (!excStatus) {
            runCommandLoop();
        }
    }

    public static boolean runCommandLoop() {

        boolean userFinished = false;
        System.out.println("Would you like to [S]can Files, or [E]xport Data?");
        Scanner scanner = new Scanner(System.in);

        switch (scanner.nextLine()) {
            case "S":
                userFinished = scanFiles();
                break;
            case "E":
                userFinished = exportFiles();
                break;
            default:
                System.out.println("Unrecognized command. Try again.");
        } 
        return userFinished;
    }

    public static boolean scanFiles() {
        // get user input
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter the path to the folder that contains the test files:");
        String pathString = scanner.nextLine();

        // validate user input
        if (pathString == null || pathString.trim().isEmpty()) {
            System.out.println("The path cannot be blank!");
            return false;
        }

        File testerDataDirectory = new File(pathString);

        try {

            // further input validation
            if (!testerDataDirectory.isDirectory()) {
                System.out.println("The given path is not a folder!");
                return false;
            }

            testDataArray = readTestData(testerDataDirectory);

        } catch (SecurityException err) {
            System.out.println("You do not have permission to access this folder. Try running this program with Administrator rights.");
            return false;
        }
        
        return false;
    }

    private static List<TestData> readTestData(File directory) {
        
        int numOfFiles = 0;
        int numOfCompletedFiles = 0;
        List<TestData> testDataList = new ArrayList<TestData>();;

        // provide user output to ensure program is working.
        System.out.println("Scanning directory...");
        File[] testerFiles = directory.listFiles();
        numOfFiles = testerFiles.length;
        System.out.println("Found " + numOfFiles + " files in this directory. Beginning data extraction...");

        for (numOfCompletedFiles = 0; numOfCompletedFiles < numOfFiles; numOfCompletedFiles++) {
            File file = testerFiles[numOfCompletedFiles];

            if (file.isDirectory()) {
                System.out.println("Found a directory. Skipping...");
                printProgress(numOfCompletedFiles + 1, numOfFiles);
                continue;
            }

            try {

                String fileDataString = "";

                FileReader fileReader = new FileReader(file);

                int currentChar = fileReader.read();
                while (currentChar != -1 && fileDataString.length() < 25000) {
                    fileDataString += ((char) currentChar);
                    currentChar = fileReader.read();
                }

                // Store Regex Matched Values
                float testTime = 0;
                String testStatus = null;
                boolean testTimeMatched = false;
                boolean testStatusMatched = false;
                
                // Match the test time
                String htmlRegexPatternString = "<td class='hdr_value'><b>(\\d+(\\.\\d+)?) second(s)?<\\/b><\\/td>";
                Pattern htmlRegexPattern = Pattern.compile(htmlRegexPatternString);
                Matcher matcher = htmlRegexPattern.matcher(fileDataString);

                if (matcher.find()) {
                    testTimeMatched = true;
                    testTime = Float.parseFloat(matcher.group(1));
                }

                // Match the test status
                htmlRegexPatternString = "<td class='hdr_name'><b>UUT Result: </b></td><td class='hdr_value'><b><span style=\"color:([^;]+);\">([^<]+)</span></b></td>";
                htmlRegexPattern = Pattern.compile(htmlRegexPatternString);
                matcher = htmlRegexPattern.matcher(fileDataString);

                if (matcher.find()) {
                    testStatusMatched = true;
                    testStatus = matcher.group(2);
                }

                // skip if could not find a match.
                if (!testTimeMatched && !testStatusMatched) {
                    continue;
                } else {
                    TestData testData = new TestData(testStatus, testTime);
                    testDataList.add(testData);
                }

            } catch(FileNotFoundException err) {
                System.out.println("This file could not be found: " + file.getAbsolutePath());
                System.out.println("Skipping...");

                printProgress(numOfCompletedFiles + 1, numOfFiles);
            } catch (IOException err) {
                System.out.println("An Unknown IO Exception Occurred! Try again.");
                throw new IllegalStateException(err.fillInStackTrace());
            }
        }
        return testDataList;
    }

    private static void printProgress(int current, int total) {
        float percentage = (float) current / (float) total;
        percentage = Math.round(percentage);

        System.out.println("Current Progress: " + current + " / " + total + " (" + percentage + "%)");
    }

    public static boolean exportFiles() {
        
        String csvString = "";
        boolean fileSucess = false;

        // BUILD THE CSV
        csvString += "Test Time, Test Status, \n";
        for (TestData testData: testDataArray) {
            csvString += testData.getTestTime() + "," + testData.getTestStatus() + ",\n";
        }

        try {
            String home = System.getProperty("user.home");

            long UNIXEpoch = convertLocalDateTimeToUnixEpoch();

            File csvFile = new File(home + "/Downloads/export" + UNIXEpoch +".csv");
            fileSucess = csvFile.createNewFile();

            if (!fileSucess) {
                throw new IllegalStateException();
            }

            // Write the data to the file.
            FileWriter fileWriter = new FileWriter(csvFile);
            fileWriter.write(csvString);
            fileWriter.close();
            System.out.println("The file was succesfully created in the Downloads folder.");

        } catch (Exception err) {
            System.out.println("An unexpected exception ocurred while exporting the file. Try again!");
        }

        return fileSucess;
    }

    public static long convertLocalDateTimeToUnixEpoch() {
        // Create a LocalDateTime instance (example)
        LocalDateTime localDateTime = LocalDateTime.now();

        // Convert LocalDateTime to Instant
        Instant instant = localDateTime.toInstant(ZoneOffset.UTC);

        // Get epoch second from Instant
        long epochSecond = instant.getEpochSecond();

        return epochSecond;
    }
}
