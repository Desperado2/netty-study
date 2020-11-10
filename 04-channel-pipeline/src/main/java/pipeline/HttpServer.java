package pipeline;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;

import java.net.InetSocketAddress;


public class HttpServer {

    public void start(int port) throws Exception{
        /**
         * NioEventLoopGroup 可以设置参数，，表示不同的线程模式
         * new NioEventLoopGroup(1) 表示单线程模式
         * new NioEventLoopGroup()  表示多线程模式，不设置数量默认开启CPU核数2倍的线程，也可以指定线程数量
         *
         *在大多数场景下，我们采用的都是主从多线程 Reactor 模型。Boss 是主 Reactor，Worker 是从 Reactor。
         * 它们分别使用不同的 NioEventLoopGroup，主 Reactor 负责处理 Accept，然后把 Channel 注册到从 Reactor 上，
         * 从 Reactor 主要负责 Channel 生命周期内的所有 I/O 事件。
         */
        EventLoopGroup boosGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(boosGroup,workerGroup)
                    // 设置channel类型，netty推荐服务端使用NioServerSocketChannel，客户端采用NioSocketChannel。
                    // 也可以使用其他的，比如OioServerSocketChannel或者EpollServerSocketChannel等.
                    .channel(NioServerSocketChannel.class)
                    .localAddress(new InetSocketAddress(port))
                    // 注册 ChannelHandler，可以使用pipeline注册多个ChannelHandler
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            // Channel初始化的时候会绑定一个pipeline用于服务编排。
                            // IO事件会依次在ChannelHandler中传播，入正向传播，出反向传播
                            socketChannel.pipeline()
                                    // http编解码
                                    .addLast("codec",new HttpServerCodec())
                                    // HttpContent 压缩
                                    .addLast("compressor",new HttpContentCompressor())
                                    // HTTP 消息聚合
                                    .addLast("aggregator",new HttpObjectAggregator(65535));
                                    // 自定义业务逻辑处理器


                            //，Inbound 事件和 Outbound 事件的传播方向是不一样的。
                            // Inbound 事件的传播方向为 Head -> Tail，
                            socketChannel.pipeline()
                                    .addLast(new SampleInBoundHandler("SampleInBoundHandlerA",false))
                                    .addLast(new SampleInBoundHandler("SampleInBoundHandlerB",false))
                                    .addLast(new SampleInBoundHandler("SampleInBoundHandlerC",true));

                            // Outbound 事件传播方向是 Tail -> Head，两者恰恰相反。
                            socketChannel.pipeline()
                                    .addLast(new SampleOutBoundHandler("SampleOutBoundHandlerA"))
                                    .addLast(new SampleOutBoundHandler("SampleOutBoundHandlerB"))
                                    .addLast(new SampleOutBoundHandler("SampleOutBoundHandlerC"));
                        }
                    })
                    // 设Channel参数，option方法用于给bossGroup设置参数，childOption用于给workerGroup设置参数
                    .childOption(ChannelOption.SO_KEEPALIVE,true);

            ChannelFuture f = b
                    // 端口绑定，正真触发启动
                    .bind()
                    // 阻塞，直到整个启动过程完成。
                    .sync();
            System.out.println("Http Server started, Listening on " + port);
            // 保让线程进入wait状态，保持服务端一直处于运行状态
            f.channel().closeFuture().sync();
        }finally {
            // 关闭连接
            workerGroup.shutdownGracefully();
            boosGroup.shutdownGracefully();
        }
    }


    public static void main(String[] args) throws Exception {
        new HttpServer().start(8088);
    }
}
