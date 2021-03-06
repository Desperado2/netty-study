
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpContent;
import io.netty.util.CharsetUtil;



public class HttpClientHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if(msg instanceof HttpContent){
            HttpContent context = (HttpContent)msg;
            ByteBuf buf = context.content();
            System.out.println(buf.toString(CharsetUtil.UTF_8));
            buf.release();
        }
    }
}
