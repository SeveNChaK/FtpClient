import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class DataReader extends Thread {

    private final Socket socket;

    private CmdOneArg currentCommand;
    private DataReaderActionListener actionListener;

    public DataReader(Socket socket, DataReaderActionListener actionListener) {
        this.socket = socket;
        this.actionListener = actionListener;
    }

    public void setCurrentCommand(CmdOneArg currentCommand) {
        this.currentCommand = currentCommand;
    }

    @Override
    public void run() {
        try {
            if (currentCommand.getCommand().equalsIgnoreCase("stor")) {
                startUpload();
            } else if (currentCommand.getCommand().equalsIgnoreCase("retr")) {
                startDownloadReader();
            } else if (
                    currentCommand.getCommand().equalsIgnoreCase("list")
                            || currentCommand.getCommand().equalsIgnoreCase("nlst")
            ) {
                startTextReader();
            }

            socket.close();
        } catch (IOException e) {
            System.out.println("Выполнение прервано! Данные не сохранились.");
        } finally {
            actionListener.onDataReaderFinish();
        }
    }

    private void startTextReader() throws IOException {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream())
        );

        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
        }

        reader.close();
    }

    private void startDownloadReader() throws IOException {
        File file = new File(currentCommand.getArgument());
        FileOutputStream fileOutputStream = new FileOutputStream(file);

        byte[] buffer = new byte[1024];
        int readCount;
        while ((readCount = socket.getInputStream().read(buffer)) != -1) {
            fileOutputStream.write(buffer, 0, readCount);
        }
        fileOutputStream.close();
        System.out.println("Файл скачан.");
    }

    private void startUpload() throws IOException {
        File file = new File(currentCommand.getArgument());
        FileInputStream fileInputStream = new FileInputStream(file);

        byte[] buffer = new byte[1024];
        int readCount;
        while ((readCount = fileInputStream.read(buffer)) != -1) {
            socket.getOutputStream().write(buffer, 0, readCount);
        }
        fileInputStream.close();
        System.out.println("Файл загружен на сервер.");
    }
}
