package WebSocket;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslHandler;

public class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private String  webSocketPath;

    public HttpRequestHandler(String webSocketPath) {
        this.webSocketPath = webSocketPath;
    }

    //处理入站请求
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        //处理坏的http请求,解码失败
        if (!request.decoderResult().isSuccess()){
            sendResponse(ctx,request, new DefaultFullHttpResponse
                    (request.protocolVersion(),HttpResponseStatus.BAD_REQUEST,ctx.alloc().buffer(0)));
            return;
        }

        //只允许get请求
        if (!HttpMethod.GET.equals(request.method())){
            sendResponse(ctx,request,new DefaultFullHttpResponse
                    (request.protocolVersion(),HttpResponseStatus.FORBIDDEN,ctx.alloc().buffer(0)));
            return;
        }

        //发送首页
        if ("/".equals(request.uri())||"/index.html".equals(request.uri())){
            String webSocketLocation = getWebSocketLocation(ctx.pipeline(),request,webSocketPath);
            ByteBuf content = IndexPage.getContent(webSocketLocation);
            FullHttpResponse response = new DefaultFullHttpResponse(request.protocolVersion(),HttpResponseStatus.OK,content);
            response.headers().set(HttpHeaderNames.CONTENT_TYPE,"text/html; charset=UTF-8");
            HttpUtil.setContentLength(response,content.readableBytes());

            sendResponse(ctx,request,response);
        }else {
            sendResponse(ctx,request,new DefaultFullHttpResponse
                    (request.protocolVersion(),HttpResponseStatus.NOT_FOUND,ctx.alloc().buffer(0)));
        }
    }

    //处理异常
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println("客户端"+ctx.channel().remoteAddress()+"异常");
        cause.printStackTrace();
        ctx.close();
    }

    //发送响应报文
    private static void sendResponse(ChannelHandlerContext ctx,FullHttpRequest request,FullHttpResponse response){
        //Generate an error page if response getStatus code is not OK (200).
        if (response.status().code()!=200){
            ByteBufUtil.writeUtf8(response.content(),response.status().toString());
            HttpUtil.setContentLength(response,response.content().readableBytes());
        }
        // Send the response and close the connection if necessary.
        boolean keepAlive = HttpUtil.isKeepAlive(request)&&response.status().code()==200;
        HttpUtil.setKeepAlive(response,keepAlive);
        ChannelFuture future = ctx.writeAndFlush(response);

        if (!keepAlive){
            future.addListener(ChannelFutureListener.CLOSE);
        }
    }

    //获取WebSocket地址
    private static String getWebSocketLocation(ChannelPipeline cp,FullHttpRequest request,String webSocketPath){
        String protocol = "ws";
        //SSL in use so use Secure WebSockets
        if (cp.get(SslHandler.class)!=null){
            protocol = "wss";
        }
        return protocol+":"+ request.headers().get(HttpHeaderNames.HOST)+webSocketPath;
    }
}
