import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import testsmell.AbstractSmell;
import testsmell.ResultsWriter;
import testsmell.TestFile;
import testsmell.TestSmellDetector;
import thresholds.DefaultThresholds;

import java.io.*;
import java.nio.file.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Mojo(name = "Detect")
public class MainMojo extends AbstractMojo {
    public void execute() throws MojoExecutionException, MojoFailureException {
        TestSmellDetector testSmellDetector = new TestSmellDetector(new DefaultThresholds());
        String directorioInicial = "src/test";
        // Llama a la función recursiva para buscar
        Optional<Path> directorioConJava = buscarDirectorioJava(Paths.get(directorioInicial));
        String directorios=directorioConJava.get().toString();
        File[] listaJava = new File(directorios).listFiles();
        List<String> pathsList = new ArrayList<>();

        for (int i = 0; i < listaJava.length; i++) {
            File archivo = listaJava[i];
            String separator = FileSystems.getDefault().getSeparator();
            String fileName = archivo.getName();
            String mainDir = directorios.replace("src" + separator + "test", "src" + separator + "main");
            String filePath = "Tests," + directorios + separator + fileName + "," +
                    mainDir + separator + fileName.substring(0, fileName.length() - 9) + ".java";

            if (i != listaJava.length - 1) {
                filePath += "\n";
            }

            pathsList.add(filePath);
        }

        String paths = String.join("", pathsList);


        String[] linesItem;
        String[] lineItem;
        TestFile testFile;
        List<TestFile> testFiles = new ArrayList<>();
        // use comma as separator
        linesItem = paths.split("\n");
        for (int i = 0; i < linesItem.length; i++) {
            lineItem = linesItem[i].split(",");

            //check if the test file has an associated production file
            if (lineItem.length == 2) {
                testFile = new TestFile(lineItem[0], lineItem[1], "");
            } else {
                testFile = new TestFile(lineItem[0], lineItem[1], lineItem[2]);
            }

            testFiles.add(testFile);
        }

        /*
          Initialize the output file - Create the output file and add the column names
         */
        ResultsWriter resultsWriter = null;
        try {
            resultsWriter = ResultsWriter.createResultsWriter();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        List<String> columnNames;
        List<String> columnValues;

        columnNames = testSmellDetector.getTestSmellNames();
        columnNames.add(0, "App");
        columnNames.add(1, "TestClass");
        columnNames.add(2, "TestFilePath");
        columnNames.add(3, "ProductionFilePath");
        columnNames.add(4, "RelativeTestFilePath");
        columnNames.add(5, "RelativeProductionFilePath");
        columnNames.add(6, "NumberOfMethods");

        try {
            resultsWriter.writeColumnName(columnNames);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        /*
          Iterate through all test files to detect smells and then write the output
        */
        TestFile tempFile;
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date;
        for (TestFile file : testFiles) {
            date = new Date();
            System.out.println(dateFormat.format(date) + " Processing: " + file.getTestFilePath());
            System.out.println("Processing: " + file.getTestFilePath());

            //detect smells
            try {
                tempFile = testSmellDetector.detectSmells(file);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            //write output
            columnValues = new ArrayList<>();
            columnValues.add(file.getApp());
            columnValues.add(file.getTestFileName());
            columnValues.add(file.getTestFilePath());
            columnValues.add(file.getProductionFilePath());
            columnValues.add(file.getRelativeTestFilePath());
            columnValues.add(file.getRelativeProductionFilePath());
            columnValues.add(String.valueOf(file.getNumberOfTestMethods()));
            for (AbstractSmell smell : tempFile.getTestSmells()) {
                try {
                    columnValues.add(String.valueOf(smell.getNumberOfSmellyTests()));
                } catch (NullPointerException e) {
                    columnValues.add("");
                }
            }
            try {
                resultsWriter.writeLine(columnValues);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        System.out.println("end");
    }

    public static Optional<Path> buscarDirectorioJava(Path directorio) {
        try {
            return Files.walk(directorio)
                    .filter(Files::isDirectory)
                    .filter(path -> {
                        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(path, "*.java")) {
                            return directoryStream.iterator().hasNext();
                        } catch (IOException e) {
                            return false;
                        }
                    })
                    .findFirst();
        } catch (IOException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }
}
