import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.FixedLengthFrameDecoder;

public class DelimiterBasedFrameDecoderServer {

    public void startEchoServer(int port) throws Exception{
        NioEventLoopGroup bossGroup = new NioEventLoopGroup();
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup,workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            /**
                             * 使用特殊分隔符解码器 DelimiterBasedFrameDecoder 之前我们需要了解以下几个属性的作用
                             *
                             * delimiters : delimiters 指定特殊分隔符，通过写入 ByteBuf 作为参数传入。delimiters 的类型是 ByteBuf 数组，
                             * 所以我们可以同时指定多个分隔符，但是最终会选择长度最短的分隔符进行消息拆分。
                             *
                             * maxLength : masLength 是报文最大长度的限制。如果超过 maxLength 还没有检测到指定分隔符，将会抛出 TooLongFrameException。
                             * 可以说 maxLength 是对程序在极端情况下的一种保护措施。
                             *
                             * failFast : failFast 与 maxLength 需要搭配使用，通过设置 failFast 可以控制抛出 TooLongFrameException 的时机，
                             * 可以说 Netty 在细节上考虑得面面俱到。如果 failFast=true，那么在超出 maxLength 会立即抛出 TooLongFrameException，不再继续进行解码。
                             * 如果 failFast=false，那么会等到解码出一个完整的消息后才会抛出 TooLongFrameException。
                             *
                             * stripDelimiter : stripDelimiter 的作用是判断解码后得到的消息是否去除分隔符。
                             *
                             * 下面我们通过一个例子感受一下使用 Netty 实现特殊分隔符解码是多么简单。
                             */
                            ByteBuf delimiter = Unpooled.copiedBuffer("&".getBytes());
                            socketChannel.pipeline().addLast(new DelimiterBasedFrameDecoder(10,true,true,delimiter));
                            socketChannel.pipeline().addLast(new EchoServerHandler());
                        }
                    });
            ChannelFuture f = b.bind(port).sync();
            f.channel().closeFuture().sync();
        }finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }


    public static void main(String[] args) throws Exception {
        new DelimiterBasedFrameDecoderServer().startEchoServer(8088);
    }
}
