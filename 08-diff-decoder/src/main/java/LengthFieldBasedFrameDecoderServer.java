import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.FixedLengthFrameDecoder;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

public class LengthFieldBasedFrameDecoderServer {

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
                             * 长度域解码器 LengthFieldBasedFrameDecoder 是解决 TCP 拆包/粘包问题最常用的**解码器。**它基本上可以覆盖大部分基于长度拆包场景，
                             * 开源消息中间件 RocketMQ 就是使用 LengthFieldBasedFrameDecoder 进行解码的。
                             * LengthFieldBasedFrameDecoder 相比 FixedLengthFrameDecoder 和 DelimiterBasedFrameDecoder 要复杂一些，
                             * 接下来我们就一起学习下这个强大的解码器。
                             * 首先我们同样先了解 LengthFieldBasedFrameDecoder 中的几个重要属性，这里我主要把它们分为两个部分：
                             * 长度域解码器特有属性以及与其他解码器（如特定分隔符解码器）的相似的属性。
                             *
                             * 长度域解码器特有属性。
                             * lengthFieldOffset : 长度字段的偏移量，也就是存放长度数据的起始位置
                             * lengthFieldLength : 长度字段所占用的字节数
                             * lengthAdjustment : 消息长度的修正值,= 包体的长度值 - 长度域的值
                             * initialBytesToStrip : 解码后需要跳过的初始字节数，也就是消息内容字段的起始位置
                             * lengthFieldEndOffset : 长度字段结束的偏移量，lengthFieldEndOffset = lengthFieldOffset + lengthFieldLength
                             *
                             * 与固定长度解码器和特定分隔符解码器相似的属性。
                             * maxFrameLength : 报文最大限制长度
                             * failFast : 是否立即抛出 TooLongFrameException，与 maxFrameLength 搭配使用
                             * discardingTooLongFrame : 是否处于丢弃模式
                             * tooLongFrameLength : 需要丢弃的字节数
                             * bytesToDiscard : 累计丢弃的字节数
                             *
                             * 示例 1：典型的基于消息长度 + 消息内容的解码。
                             * BEFORE DECODE (14 bytes)         AFTER DECODE (14 bytes)
                             * +--------+----------------+      +--------+----------------+
                             * | Length | Actual Content |----->| Length | Actual Content |
                             * | 0x000C | "HELLO, WORLD" |      | 0x000C | "HELLO, WORLD" |
                             * +--------+----------------+      +--------+----------------+
                             * 上述协议是最基本的格式，报文只包含消息长度 Length 和消息内容 Content 字段，其中 Length 为 16 进制表示，
                             * 共占用 2 字节，Length 的值 0x000C 代表 Content 占用 12 字节。该协议对应的解码器参数组合如下：
                             *
                             * lengthFieldOffset = 0，因为 Length 字段就在报文的开始位置。
                             * lengthFieldLength = 2，协议设计的固定长度。
                             * lengthAdjustment = 0，Length 字段只包含消息长度，不需要做任何修正。
                             * initialBytesToStrip = 0，解码后内容依然是 Length + Content，不需要跳过任何初始字节。
                             *
                             * 示例 2：解码结果需要截断。
                             * BEFORE DECODE (14 bytes)         AFTER DECODE (12 bytes)
                             * +--------+----------------+      +----------------+
                             * | Length | Actual Content |----->| Actual Content |
                             * | 0x000C | "HELLO, WORLD" |      | "HELLO, WORLD" |
                             * +--------+----------------+      +----------------+
                             * 示例 2 和示例 1 的区别在于解码后的结果只包含消息内容，其他的部分是不变的。该协议对应的解码器参数组合如下：
                             *
                             * lengthFieldOffset = 0，因为 Length 字段就在报文的开始位置。
                             * lengthFieldLength = 2，协议设计的固定长度。
                             * lengthAdjustment = 0，Length 字段只包含消息长度，不需要做任何修正
                             * initialBytesToStrip = 2，跳过 Length 字段的字节长度，解码后 ByteBuf 中只包含 Content字段。
                             *
                             * 示例 3：长度字段包含消息长度和消息内容所占的字节。
                             * BEFORE DECODE (14 bytes)         AFTER DECODE (14 bytes)
                             * +--------+----------------+      +--------+----------------+
                             * | Length | Actual Content |----->| Length | Actual Content |
                             * | 0x000E | "HELLO, WORLD" |      | 0x000E | "HELLO, WORLD" |
                             * +--------+----------------+      +--------+----------------+
                             * 与前两个示例不同的是，示例 3 的 Length 字段包含 Length 字段自身的固定长度以及 Content 字段所占用的字节数，
                             * Length 的值为 0x000E（2 + 12 = 14 字节），在 Length 字段值（14 字节）的基础上做 lengthAdjustment（-2）的修正，
                             * 才能得到真实的 Content 字段长度，所以对应的解码器参数组合如下：
                             *
                             * lengthFieldOffset = 0，因为 Length 字段就在报文的开始位置。
                             * lengthFieldLength = 2，协议设计的固定长度。
                             * lengthAdjustment = -2，长度字段为 14 字节，需要减 2 才是拆包所需要的长度。
                             * initialBytesToStrip = 0，解码后内容依然是 Length + Content，不需要跳过任何初始字节。
                             *
                             * 示例 4：基于长度字段偏移的解码。
                             * BEFORE DECODE (17 bytes)                      AFTER DECODE (17 bytes)
                             * +----------+----------+----------------+      +----------+----------+----------------+
                             * | Header 1 |  Length  | Actual Content |----->| Header 1 |  Length  | Actual Content |
                             * |  0xCAFE  | 0x00000C | "HELLO, WORLD" |      |  0xCAFE  | 0x00000C | "HELLO, WORLD" |
                             * +----------+----------+----------------+      +----------+----------+----------------+
                             * 示例 4 中 Length 字段不再是报文的起始位置，Length 字段的值为 0x00000C，表示 Content 字段占用 12 字节，
                             * 该协议对应的解码器参数组合如下：
                             *
                             * lengthFieldOffset = 2，需要跳过 Header 1 所占用的 2 字节，才是 Length 的起始位置。
                             * lengthFieldLength = 3，协议设计的固定长度。
                             * lengthAdjustment = 0，Length 字段只包含消息长度，不需要做任何修正。
                             * initialBytesToStrip = 0，解码后内容依然是完整的报文，不需要跳过任何初始字节。
                             *
                             * 示例 5：长度字段与内容字段不再相邻。
                             * BEFORE DECODE (17 bytes)                      AFTER DECODE (17 bytes)
                             * +----------+----------+----------------+      +----------+----------+----------------+
                             * |  Length  | Header 1 | Actual Content |----->|  Length  | Header 1 | Actual Content |
                             * | 0x00000C |  0xCAFE  | "HELLO, WORLD" |      | 0x00000C |  0xCAFE  | "HELLO, WORLD" |
                             * +----------+----------+----------------+      +----------+----------+----------------+
                             * 示例 5 中的 Length 字段之后是 Header 1，Length 与 Content 字段不再相邻。Length 字段所表示的内容略过了 Header 1 字段，
                             * 所以也需要通过 lengthAdjustment 修正才能得到 Header + Content 的内容。示例 5 所对应的解码器参数组合如下：
                             *
                             * lengthFieldOffset = 0，因为 Length 字段就在报文的开始位置。
                             * lengthFieldLength = 3，协议设计的固定长度。
                             * lengthAdjustment = 2，由于 Header + Content 一共占用 2 + 12 = 14 字节，
                             * 所以 Length 字段值（12 字节）加上 lengthAdjustment（2 字节）才能得到 Header + Content 的内容（14 字节）。
                             * initialBytesToStrip = 0，解码后内容依然是完整的报文，不需要跳过任何初始字节。
                             *
                             * 示例 6：基于长度偏移和长度修正的解码。
                             * BEFORE DECODE (16 bytes)                       AFTER DECODE (13 bytes)
                             * +------+--------+------+----------------+      +------+----------------+
                             * | HDR1 | Length | HDR2 | Actual Content |----->| HDR2 | Actual Content |
                             * | 0xCA | 0x000C | 0xFE | "HELLO, WORLD" |      | 0xFE | "HELLO, WORLD" |
                             * +------+--------+------+----------------+      +------+----------------+
                             * 示例 6 中 Length 字段前后分为别 HDR1 和 HDR2 字段，各占用 1 字节，所以既需要做长度字段的偏移，也需要做 lengthAdjustment 修正，
                             * 具体修正的过程与 示例 5 类似。对应的解码器参数组合如下：
                             *
                             * lengthFieldOffset = 1，需要跳过 HDR1 所占用的 1 字节，才是 Length 的起始位置。
                             * lengthFieldLength = 2，协议设计的固定长度。
                             * lengthAdjustment = 1，由于 HDR2 + Content 一共占用 1 + 12 = 13 字节，
                             * 所以 Length 字段值（12 字节）加上 lengthAdjustment（1）才能得到 HDR2 + Content 的内容（13 字节）。
                             * initialBytesToStrip = 3，解码后跳过 HDR1 和 Length 字段，共占用 3 字节。
                             *
                             * 示例 7：长度字段包含除 Content 外的多个其他字段。
                             * BEFORE DECODE (16 bytes)                       AFTER DECODE (13 bytes)
                             * +------+--------+------+----------------+      +------+----------------+
                             * | HDR1 | Length | HDR2 | Actual Content |----->| HDR2 | Actual Content |
                             * | 0xCA | 0x0010 | 0xFE | "HELLO, WORLD" |      | 0xFE | "HELLO, WORLD" |
                             * +------+--------+------+----------------+      +------+----------------+
                             * 示例 7 与 示例 6 的区别在于 Length 字段记录了整个报文的长度，包含 Length 自身所占字节、HDR1 、HDR2 以及 Content 字段的长度，
                             * 解码器需要知道如何进行 lengthAdjustment 调整，才能得到 HDR2 和 Content 的内容。所以我们可以采用如下的解码器参数组合：
                             *
                             * lengthFieldOffset = 1，需要跳过 HDR1 所占用的 1 字节，才是 Length 的起始位置。
                             * lengthFieldLength = 2，协议设计的固定长度。
                             * lengthAdjustment = -3，Length 字段值（16 字节）需要减去 HDR1（1 字节） 和 Length 自身所占字节长度（2 字节）才能得到 HDR2 和 Content 的内容（1 + 12 = 13 字节）。
                             * initialBytesToStrip = 3，解码后跳过 HDR1 和 Length 字段，共占用 3 字节。
                             */
                            socketChannel.pipeline().addLast(new LengthFieldBasedFrameDecoder(50,0,1));
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
        new LengthFieldBasedFrameDecoderServer().startEchoServer(8088);
    }
}
