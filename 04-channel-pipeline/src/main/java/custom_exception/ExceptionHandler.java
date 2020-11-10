package custom_exception;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;


public class ExceptionHandler extends ChannelDuplexHandler {

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if(cause instanceof RuntimeException){
            System.out.println("Handle Business Exception Success.");
        }
    }
}
