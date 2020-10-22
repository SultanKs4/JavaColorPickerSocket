import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.atomic.AtomicInteger;

public class Client {
    private JTextField textFieldIPAddress;
    private JTextField textFieldPort;
    private JTextField textFieldRed;
    private JTextField textFieldGreen;
    private JTextField textFieldBlue;
    private JButton buttonSetColorPicker;
    private JButton buttonSend;
    private JPanel root;
    private JPanel panelColor;
    private String colorCodeFinal = "0,0,0";

    public Client() {
        buttonSetColorPicker.addActionListener(actionEvent -> setColorCode());
        buttonSend.addActionListener(actionEvent -> sendData());
    }

    private void setColorCode() {
        Color selectedColor = colorPicker();
        panelColor.setBackground(selectedColor);

        textFieldRed.setText(String.valueOf(selectedColor.getRed()));
        textFieldGreen.setText(String.valueOf(selectedColor.getGreen()));
        textFieldBlue.setText(String.valueOf(selectedColor.getBlue()));

        colorCodeFinal = textFieldRed.getText() + "," +
                textFieldGreen.getText() + "," +
                textFieldBlue.getText() + ",";
    }

    private Color colorPicker() {
        Color current = Color.BLACK;
        if (panelColor.getBackground() != null) {
            current = panelColor.getBackground();
        }
        Color resultColor = JColorChooser.showDialog(null, "Choose Color", current);
        if (resultColor == null) {
            resultColor = current;
        }
        return resultColor;
    }

    private void sendData() {
        try {
            AtomicInteger messageWritten = new AtomicInteger(0);
            AtomicInteger messageRead = new AtomicInteger(0);

            EchoClient(textFieldIPAddress.getText(), Integer.parseInt(textFieldPort.getText()), colorCodeFinal, messageWritten, messageRead);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, ex.getMessage());
        }
    }

    //////////////////////////
    // Socket Client Helper //
    //////////////////////////
    public void EchoClient(String host, int port, final String message, final AtomicInteger messageWritten, final AtomicInteger messageRead) throws IOException {
        //create a socket channel
        AsynchronousSocketChannel sockChannel = AsynchronousSocketChannel.open();

        //try to connect to the server side
        sockChannel.connect(new InetSocketAddress(host, port), sockChannel, new CompletionHandler<Void, AsynchronousSocketChannel>() {
            @Override
            public void completed(Void result, AsynchronousSocketChannel channel) {
                //start to read message
                startRead(channel, messageRead);

                //write an message to server side
                startWrite(channel, String.valueOf(message), messageWritten);
            }

            @Override
            public void failed(Throwable exc, AsynchronousSocketChannel channel) {
                System.out.println("fail to connect to server");
            }

        });
    }

    private void startRead(final AsynchronousSocketChannel sockChannel, final AtomicInteger messageRead) {
        final ByteBuffer buf = ByteBuffer.allocate(2048);

        sockChannel.read(buf, sockChannel, new CompletionHandler<Integer, AsynchronousSocketChannel>() {

            @Override
            public void completed(Integer result, AsynchronousSocketChannel channel) {
                //message is read from server
                messageRead.getAndIncrement();

                //print the message
                System.out.println("Read message:" + new String(buf.array()));
            }

            @Override
            public void failed(Throwable exc, AsynchronousSocketChannel channel) {
                System.out.println("fail to read message from server");
            }
        });
    }

    private void startWrite(final AsynchronousSocketChannel sockChannel, final String message, final AtomicInteger messageWritten) {
        ByteBuffer buf = ByteBuffer.allocate(2048);
        buf.put(message.getBytes());
        buf.flip();
        messageWritten.getAndIncrement();
        sockChannel.write(buf, sockChannel, new CompletionHandler<Integer, AsynchronousSocketChannel>() {
            @Override
            public void completed(Integer result, AsynchronousSocketChannel channel) {
                //after message written
                //NOTHING TO DO
            }

            @Override
            public void failed(Throwable exc, AsynchronousSocketChannel channel) {
                System.out.println("Fail to write the message to server");
            }
        });
    }

    public static void main(String[] args) {
        JFrame gui = new JFrame("Color Picker Client");
        gui.setContentPane(new Client().root);
        gui.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        gui.pack();
        gui.setVisible(true);
    }
}
