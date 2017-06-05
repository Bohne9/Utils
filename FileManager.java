package FileTransfer;

import Protocol.DataPacket;
import Protocol.ProtocolConnection;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;

import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by jonahschueller on 03.06.17.
 */
public class FileManager {
    private static int BUFFERSIZE = 4096;

    private DoubleProperty sendProcess, receiveProcess;
    private double currentSize, totalReceive;
    private LinkedList<File> inputFiles, outputFiles;
    private File inputFile, outputFile;
    private Socket socket;
    private Thread receiver, sender;
    private boolean sending, receiving;
    private byte[] buffer;
    private CopyOnWriteArrayList<DataPacket<String>> packets;
    private FileInputStream input;
    private FileOutputStream output;
    private ProtocolConnection protocol;

    public FileManager(Socket socket,ProtocolConnection protocol){
        this.socket = socket;
        this.protocol = protocol;
        inputFiles = new LinkedList<>();
        outputFiles = new LinkedList<>();
        packets = new CopyOnWriteArrayList<>();
        receiver = new Thread(receiveRun);
        sender = new Thread(sendRun);
        receiver.start();
        sender.start();
        buffer = allocate();
        sendProcess = new SimpleDoubleProperty();
        receiveProcess = new SimpleDoubleProperty();
    }

    private byte[] allocate(){
        return new byte[BUFFERSIZE];
    }

    public void send(File file){
        if (file != null){
            if (file.exists()){
                inputFiles.add(file);
                notify(sender);
            }
        }
    }

    public void receive(File file){
        if (file != null){
            if (!file.exists()){
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            outputFiles.add(file);
        }
    }


    public DoubleProperty getSendProcess() {
        return sendProcess;
    }

    public DoubleProperty getReceiveProcess() {
        return receiveProcess;
    }

    private void sendFile(){
        nextInputFile();
        try {
            double total = 0, size = (double)input.available();
            protocol.send(DataPacket.stringPacketBuilder(socket, "" + size, "nextfile"));
            while(input.available() > 0){
                byte[] b = read();
                total += b.length;
                sendProcess.setValue((total / size));
                protocol.send(dataPacketBuilder(b));
            }
            protocol.send(DataPacket.stringPacketBuilder(socket, inputFile.getName(), "finished"));
            System.out.println("FINISHED");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addDataPacket(DataPacket<String> packet){
        if (packet != null){
            packets.add(packet);
            notify(receiver);
        }
    }

    private void write(DataPacket<String> packet){
        try {
            totalReceive += packet.dataLength();
            receiveProcess.setValue((totalReceive / currentSize));
            write(packet.getData());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void finished(){
        try {
            output.flush();
            output.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    //Sending
    public void nextInputFile(){
        sendProcess.setValue(0);
        inputFile = inputFiles.pollFirst();
        try {
            input = new FileInputStream(inputFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    //Receiving
    public boolean nextOutputFile(){
        if (output != null){
            finished();
        }

        receiveProcess.setValue(0);
        outputFile = outputFiles.pollFirst();
        if (outputFile == null){
            return false;
        }
        try {
            output = new FileOutputStream(outputFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return true;
    }

    public void setCurrentSize(double currentSize) {
        this.currentSize = currentSize;
        totalReceive = 0;
    }

    private void write(byte[] bytes) throws IOException{
        output.write(bytes);
    }

    private byte[] read() throws IOException{
        byte[] buffer = allocate();
        int val = input.read(buffer);
        if (val < BUFFERSIZE){
            return Arrays.copyOf(buffer, val);
        }
        return buffer;
    }

    private DataPacket<String> dataPacketBuilder(byte[] data){
        return DataPacket.stringPacketBuilder(socket, data, "data");
    }

    private Runnable sendRun = ()->{
        sending = true;
        while (sending){
            while(!inputFiles.isEmpty()){
                sendFile();
            }
            wait(sender);
        }
    };

    private Runnable receiveRun = ()->{
        receiving = true;
        while (receiving){
            if (!packets.isEmpty()){
                DataPacket<String> packet = packets.get(0);
                packets.remove(0);
                write(packet);
            }else {
                wait(receiver);
            }
        }
    };

    private void wait(Object obj){
        synchronized (obj){
            try {
                obj.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void notify(Object obj){
        synchronized (obj){
            obj.notify();
        }
    }
}
