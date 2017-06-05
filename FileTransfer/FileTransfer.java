package FileTransfer;

import Protocol.DataPacket;
import Protocol.ProtocolHandler;
import Protocol.StringProtocol;
import javafx.beans.property.DoubleProperty;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.NotDirectoryException;

/**
 * Created by jonahschueller on 03.06.17.
 */
public class FileTransfer {


    private FileManager fileManager;
    private Socket socket;
    private String path;
    private Hook newFileHook;
    private Hook<String> finiHook;
    private ProtocolHandler<String> ftpHandler;


    public FileTransfer(Socket socket){
        this.socket = socket;
        ftpHandler = new ProtocolHandler<String>(ftp);
        fileManager = new FileManager(socket, ftpHandler);
        ftpHandler.start();
        ftpHandler.listen(socket, ()->ftpHandler.stopListening());
    }

    public FileTransfer(String ip, int port) throws IOException {
        this(new Socket(ip, port));
    }

    public void send(File file){
        if (file != null){
            if(file.isFile()){
                sendFile(file);
            }else if(file.isDirectory()){
                sendDir(file);
            }
        }
    }

    public DoubleProperty getSendProcess(){
        return fileManager.getSendProcess();
    }

    public DoubleProperty getReceiveProcess(){
        return fileManager.getReceiveProcess();
    }

    public void send(String path){
        if (path != null)
            send(new File(path));
    }

    private void sendFile(File file){
        DataPacket<String> packet = DataPacket.stringPacketBuilder(socket, file.getName().getBytes(), "newfile");
        ftpHandler.send(packet);
        fileManager.send(file);
    }

    private void sendDir(File dir){
        if (dir != null){
            if (dir.exists() && dir.isDirectory()){
                for (File file :
                        dir.listFiles()) {
                    if (file.isFile()){
                        sendFile(file);
                    }else if (file.isDirectory()){
                        sendDir(file);
                    }
                }
            }else {
                try {
                    throw new NotDirectoryException(dir.toString());
                } catch (NotDirectoryException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    private StringProtocol ftp = new StringProtocol("ftp") {
        @Override
        protected void protocolSetup() {
            setKeyPos(0);
            addHeaderItem(8);

            createNode("data", packet -> {
                fileManager.addDataPacket(packet);
            });

            createNode("nextfile", packet -> {
                fileManager.nextOutputFile();
                fileManager.setCurrentSize(Double.parseDouble(packet.dataToString()));
                if (newFileHook != null)
                    newFileHook.call(packet.dataToString());
            });

            createNode("newfile", packet -> {
                fileManager.receive(new File(path + new String(packet.getData())));
            });

            createNode("finished", packet->{
                if (finiHook != null){
                    finiHook.call(packet.dataToString());
                }
            });

            createNode("newdire", packet -> {

            });

            createNode("close", packet -> {
                try {
                    ftpHandler.stopListening();
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }

        @Override
        protected String evaluate(DataPacket<String> packet) {
            return packet.get(0);
        }
    };

    public void setNewFileHook(Hook newFileHook) {
        this.newFileHook = newFileHook;
    }


    public void setFinishedHook(FileTransfer.Hook<String> finiHook) {
        this.finiHook = finiHook;
    }

    private interface Hook<T>{

        void call(T data);

    }


}
