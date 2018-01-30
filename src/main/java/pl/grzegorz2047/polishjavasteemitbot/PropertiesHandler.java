package pl.grzegorz2047.polishjavasteemitbot;

import java.io.*;
import java.util.Properties;

public class PropertiesHandler {
    void createFileProperties() throws IOException {
        Properties prop = new Properties();
        OutputStream output = null;

        try {
            output = new FileOutputStream("bot.properties");
            prop.setProperty("watchedTag", "polish");
            prop.setProperty("botName", "grzegorz2047");
            prop.setProperty("postingKey", "enteryourprivatepostingkey");
            prop.setProperty("message", "Welcome on this tag!");
            prop.setProperty("commentTags", "welcome,first,bot,cool");

            // save properties to project root folder
            prop.store(output, null);

        } catch (IOException io) {
            io.printStackTrace();
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    Properties getProperties() throws IOException {
        Properties prop = new Properties();
        InputStream input = null;
        try {
            input = new FileInputStream("bot.properties");

            prop.load(input);
            return prop;
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return prop;
    }
}
