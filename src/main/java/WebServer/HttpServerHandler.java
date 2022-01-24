package WebServer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;

import java.nio.charset.Charset;

public class HttpServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
        readRequest(msg);

        String sendMsg;
        String uri = msg.uri();
        switch (uri){
            case "/":
                sendMsg = "<h3>Netty Http Server</h3><a>welcome!</a>";
                break;
            case "/hello":
                sendMsg = "<h3>Netty Http Server</h3><a>hello world!</a>";
                break;
            case "/love":
                sendMsg = "<h3>Netty Http Server</h3><a>I love you!</a>";
                break;
            default:
                sendMsg = "<h3>Netty Http Server</h3><a>I was lost!</a>";
                break;
        }

        writeResponse(ctx,sendMsg);
    }

    private void readRequest(FullHttpRequest msg){
        System.out.println("======请求行======");
        System.out.println(msg.method()+" "+msg.uri()+" "+msg.protocolVersion());
        System.out.println("======请求头======");
        for (String name : msg.headers().names()){
            System.out.println(name+":"+msg.headers().get(name));
        }
        System.out.println("======消息体======");
        System.out.println(msg.content().toString(Charset.defaultCharset()));
    }

    private void writeResponse(ChannelHandlerContext ctx,String msg){
        ByteBuf bf = Unpooled.copiedBuffer(msg,Charset.defaultCharset());
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,HttpResponseStatus.OK,bf);
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH,msg.length());
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
    }
}
