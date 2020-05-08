import java.io.IOException;

public class Main {

    public static void main(String[] args) {
        try {
            FtpClient ftpClient = new FtpClient("127.0.0.1", 21);
            ftpClient.start();
        } catch (IOException e) {
            //empty
        }

        System.out.println("До свидания!");
    }
}
