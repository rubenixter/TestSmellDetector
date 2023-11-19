package testsmell;

import java.io.FileWriter;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.List;

/**
 * This class is utilized to write output to an HTML file.
 */
public class ResultsWriter {

    private String outputFile;
    private FileWriter writer;

    /**
     * Creates the file into which output it to be written into. Results from each file will be stored in a new file
     * @throws IOException
     */
    private ResultsWriter() throws IOException {
        outputFile = MessageFormat.format("{0}_{1}.{2}", "Reporte","TestSmell", "html");
        writer = new FileWriter(outputFile, false);
        writer.write("<html><head><title>Resultados</title></head><body><table border='7'>");
        writer.flush();
    }

    /**
     * Factory method that provides a new instance of the ResultsWriter
     * @return new ResultsWriter instance
     * @throws IOException
     */
    public static ResultsWriter createResultsWriter() throws IOException {
        return new ResultsWriter();
    }


    public void writeColumnName(List<String> columnNames) throws IOException {
        writer.append("<tr>");
        for (String name : columnNames) {
            writer.append("<th>").append(name).append("</th>");
        }
        writer.append("</tr>");
        writer.flush();
    }

    public void writeLine(List<String> columnValues) throws IOException {
        writer.append("<tr>");
        for (String value : columnValues) {
            writer.append("<td>").append(value).append("</td>");
        }
        writer.append("</tr>");
        writer.flush();
    }


    public void close() throws IOException {
        writer.append("</table></body></html>");
        writer.flush();
        writer.close();
    }
}
