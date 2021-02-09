import web.HttpClient;
import web.HttpResponse;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import java.util.logging.Logger;



public class MainApp {
    private static final Logger log = Logger.getLogger(MainApp.class.getName());

    public static void main(String args[]) throws IOException {

        System.out.println("Enter URL:");
        Scanner scanner = new Scanner(System.in);
        String url = scanner.nextLine();

        HttpClient httpClient = new HttpClient(url);
        HttpResponse response = httpClient.connect();
        response.checkOk();

        System.out.println("Enter the directory:");
        String strCatalog = scanner.nextLine();
        File directory = new File(strCatalog);

        HtmlWordCounter.builder(url).setSaveFile(directory).setWordDelimiters(" ", "\n").build().run(response);

    }

}