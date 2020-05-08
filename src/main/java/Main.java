import java.io.IOException;

public class Main {

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Не заданы аргументы host и port!");
            System.exit(1);
        }

        String host = args[0];
        int port = 21;
        try{
            port = Integer.valueOf(args[1]);
        } catch (NumberFormatException e) {
            System.out.println("Введен некорректный порт!");
            System.exit(1);
        }


        try {
            FtpClient ftpClient = new FtpClient(host, port);
            ftpClient.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
