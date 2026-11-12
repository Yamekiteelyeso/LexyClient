package com.lexy.client;

import com.google.gson.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Util;

/** Spotify Web API/Connect controller. No Spotify audio is downloaded or intercepted. */
public final class SpotifyController {
    public static final SpotifyController INSTANCE = new SpotifyController();
    private static final String REDIRECT = "http://127.0.0.1:43871/callback";
    private static final String SCOPES = "user-read-playback-state user-read-currently-playing user-modify-playback-state";
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {Thread t=new Thread(r,"Lexy-Spotify-IO");t.setDaemon(true);return t;});
    private final ScheduledExecutorService poller = Executors.newSingleThreadScheduledExecutor(r -> {Thread t=new Thread(r,"Lexy-Spotify-Poll");t.setDaemon(true);return t;});
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6)).build();
    private final SecureRandom random = new SecureRandom();
    private volatile String clientId="", accessToken="", refreshToken="", status="Not connected", playing="Nothing playing", pendingAuthUrl="";
    private volatile boolean paused=true;
    private volatile long expireAt=0;
    private volatile List<Track> tracks=List.of();
    private volatile List<Device> devices=List.of();
    private volatile String activeDevice="";
    private volatile HttpServer callback;
    private volatile String verifier="", state="";
    public record Track(String title,String artist,String uri) {}
    public record Device(String id,String name,String type,boolean active) {}
    private SpotifyController(){poller.scheduleWithFixedDelay(()->{if(isConnected())executor.submit(()->{try{fetchPlayback();fetchDevices();}catch(Exception e){status="Spotify: "+shortError(e);}});},1,10,TimeUnit.SECONDS);}
    public void setClientId(String id){clientId=id==null?"":id.trim();}
    public String clientId(){return clientId;}
    public boolean isConnected(){return !refreshToken.isBlank()||!accessToken.isBlank();}
    public String status(){return status;}
    public String playing(){return playing;}
    public boolean paused(){return paused;}
    public List<Track> tracks(){return tracks;}
    public List<Device> devices(){return devices;}
    public String activeDevice(){return activeDevice;}
    public String authUrl(){return pendingAuthUrl;}
    public void cancelLogin(){stopCallback();pendingAuthUrl="";verifier="";state="";status="Authorization cancelled";}
    public void copyAuthUrl(){
        if(pendingAuthUrl.isBlank()){status="Click LOGIN first";return;}
        try {MinecraftClient.getInstance().keyboard.setClipboard(pendingAuthUrl);status="Spotify login URL copied";}
        catch(Throwable error){status="Clipboard unavailable on this launcher";}
    }
    private String randomString(int length){byte[] bytes=new byte[length];random.nextBytes(bytes);return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);}
    private static String enc(String s){return URLEncoder.encode(s,StandardCharsets.UTF_8);}
    private static String shortError(Exception ex){String m=ex.getMessage();return m==null?ex.getClass().getSimpleName():m.substring(0,Math.min(120,m.length()));}
    public void connect(){
        if(clientId.isBlank()){status="Enter your Spotify Client ID first";return;}
        if(!clientId.matches("[A-Za-z0-9]{20,80}")){status="Invalid Client ID format";return;}
        if(callback!=null){status="Authorization already open";return;}
        try {
            verifier=randomString(48);state=randomString(24);
            byte[] digest=MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII));
            String challenge=Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
            HttpServer server=HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"),43871),0);
            server.createContext("/callback",this::handleCallback);
            server.setExecutor(executor);server.start();callback=server;
            pendingAuthUrl="https://accounts.spotify.com/authorize?response_type=code&client_id="+enc(clientId)+"&redirect_uri="+enc(REDIRECT)+"&scope="+enc(SCOPES)+"&state="+enc(state)+"&code_challenge_method=S256&code_challenge="+enc(challenge);
            try {
                Util.getOperatingSystem().open(URI.create(pendingAuthUrl));
                status="Browser opened. Approve Spotify login...";
            } catch (Throwable openError) {
                MinecraftClient mc = MinecraftClient.getInstance();
                try {
                    if (mc != null) mc.keyboard.setClipboard(pendingAuthUrl);
                    status="Browser unavailable. Login URL copied to clipboard.";
                } catch(Throwable clipboardError){status="Browser unavailable. Use COPY LOGIN URL in Spotify tab.";}
            }
        } catch(Exception ex){stopCallback();status="Connect failed: "+shortError(ex);} 
    }
    private static Map<String,String> query(String raw){
        HashMap<String,String> out=new HashMap<>();
        if(raw!=null)for(String p:raw.split("&")){String[] a=p.split("=",2);if(a.length==2)out.put(URLDecoder.decode(a[0],StandardCharsets.UTF_8),URLDecoder.decode(a[1],StandardCharsets.UTF_8));}
        return out;
    }
    private void handleCallback(HttpExchange exchange){
        String result="Lexy Client: Spotify login complete. Return to Minecraft.";
        try{
            if(!"GET".equals(exchange.getRequestMethod())){result="Invalid request";exchange.sendResponseHeaders(405,-1);return;}
            Map<String,String> q=query(exchange.getRequestURI().getRawQuery());
            if(!Objects.equals(q.get("state"),state)||state.isBlank()){result="Spotify authentication state mismatch";status=result;}
            else if(q.containsKey("error")){result="Spotify authorization denied";status=result;}
            else if(!q.containsKey("code")){result="Spotify authorization code missing";status=result;}
            else {
                String code=q.get("code");String authVerifier=verifier;
                JsonObject token=tokenRequest("grant_type=authorization_code&code="+enc(code)+"&redirect_uri="+enc(REDIRECT)+"&client_id="+enc(clientId)+"&code_verifier="+enc(authVerifier));
                applyTokens(token);status="Connected to Spotify";result="Spotify connected successfully. Return to Minecraft.";
            }
            byte[] bytes=("<html><body style='font-family:sans-serif;background:#111827;color:#e0e9ff;padding:40px'><h2>Lexy Client</h2><p>"+result+"</p></body></html>").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type","text/html; charset=utf-8");exchange.sendResponseHeaders(200,bytes.length);exchange.getResponseBody().write(bytes);
        }catch(Exception ex){status="Auth error: "+shortError(ex);try{byte[] b="Authentication failed; return to Minecraft".getBytes(StandardCharsets.UTF_8);exchange.sendResponseHeaders(500,b.length);exchange.getResponseBody().write(b);}catch(Exception ignored){}}
        finally{exchange.close();stopCallback();pendingAuthUrl="";verifier="";state="";}
    }
    private void stopCallback(){HttpServer old=callback;callback=null;if(old!=null)old.stop(0);}
    private JsonObject tokenRequest(String form) throws Exception {
        HttpRequest req=HttpRequest.newBuilder(URI.create("https://accounts.spotify.com/api/token"))
            .header("Content-Type","application/x-www-form-urlencoded").timeout(Duration.ofSeconds(12))
            .POST(HttpRequest.BodyPublishers.ofString(form)).build();
        HttpResponse<String> resp=http.send(req,HttpResponse.BodyHandlers.ofString());
        if(resp.statusCode()!=200)throw new IOException("Spotify OAuth HTTP "+resp.statusCode());
        return JsonParser.parseString(resp.body()).getAsJsonObject();
    }
    private synchronized void applyTokens(JsonObject o){
        accessToken=o.get("access_token").getAsString();
        if(o.has("refresh_token"))refreshToken=o.get("refresh_token").getAsString();
        expireAt=System.currentTimeMillis()+o.get("expires_in").getAsLong()*1000L-60_000L;
    }
    private synchronized String token() throws Exception {
        if(System.currentTimeMillis()>=expireAt){
            if(refreshToken.isBlank())throw new IllegalStateException("Not connected");
            applyTokens(tokenRequest("grant_type=refresh_token&refresh_token="+enc(refreshToken)+"&client_id="+enc(clientId)));
        }
        return accessToken;
    }
    private HttpResponse<String> request(String method,String path,String body) throws Exception {
        HttpRequest.Builder b=HttpRequest.newBuilder(URI.create("https://api.spotify.com/v1"+path))
            .timeout(Duration.ofSeconds(10)).header("Authorization","Bearer "+token());
        if(body!=null)b.header("Content-Type","application/json");
        b.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body));
        HttpResponse<String> r=http.send(b.build(),HttpResponse.BodyHandlers.ofString());
        if(r.statusCode()==429)throw new IOException("Rate limited (429); try later");
        if(r.statusCode()==403)throw new IOException("Spotify Premium / app access required (403)");
        if(r.statusCode()==404)throw new IOException("Start Spotify playback on a device first (404)");
        if(r.statusCode()/100!=2)throw new IOException("Spotify HTTP "+r.statusCode());
        return r;
    }
    private static String get(JsonObject o,String key){return o!=null&&o.has(key)&&!o.get(key).isJsonNull()?o.get(key).getAsString():"";}
    private void fetchPlayback() throws Exception {
        HttpResponse<String> r=request("GET","/me/player",null);
        if(r.statusCode()==204||r.body().isBlank()){playing="No active Spotify player";paused=true;activeDevice="";return;}
        JsonObject o=JsonParser.parseString(r.body()).getAsJsonObject();
        paused=!o.has("is_playing")||!o.get("is_playing").getAsBoolean();
        JsonObject device=o.has("device")&&!o.get("device").isJsonNull()?o.getAsJsonObject("device"):null;
        activeDevice=get(device,"id");
        JsonObject item=o.has("item")&&!o.get("item").isJsonNull()?o.getAsJsonObject("item"):null;
        String artist="";
        if(item!=null&&item.has("artists")&&item.getAsJsonArray("artists").size()>0)artist=get(item.getAsJsonArray("artists").get(0).getAsJsonObject(),"name");
        playing=item==null?"Nothing playing":get(item,"name")+(artist.isBlank()?"":" - "+artist);
    }
    private void fetchDevices() throws Exception {
        JsonObject root=JsonParser.parseString(request("GET","/me/player/devices",null).body()).getAsJsonObject();
        ArrayList<Device> list=new ArrayList<>();
        for(JsonElement el:root.getAsJsonArray("devices")){
            JsonObject o=el.getAsJsonObject();list.add(new Device(get(o,"id"),get(o,"name"),get(o,"type"),o.get("is_active").getAsBoolean()));
        }
        devices=List.copyOf(list);
    }
    public void search(String search){
        if(search==null||search.isBlank())return;
        executor.submit(()->{
            try{status="Searching Spotify...";
                JsonObject root=JsonParser.parseString(request("GET","/search?q="+enc(search.trim())+"&type=track&limit=10",null).body()).getAsJsonObject();
                ArrayList<Track> found=new ArrayList<>();
                for(JsonElement el:root.getAsJsonObject("tracks").getAsJsonArray("items")){
                    JsonObject t=el.getAsJsonObject();String artist="";
                    if(t.has("artists")&&t.getAsJsonArray("artists").size()>0)artist=get(t.getAsJsonArray("artists").get(0).getAsJsonObject(),"name");
                    found.add(new Track(get(t,"name"),artist,get(t,"uri")));
                }
                tracks=List.copyOf(found);status="Found "+found.size()+" tracks";
            }catch(Exception ex){status="Search failed: "+shortError(ex);}
        });
    }
    private void action(String method,String path,String body,String label){executor.submit(()->{
        try{request(method,path,body);status=label;fetchPlayback();}catch(Exception ex){status=label+": "+shortError(ex);}
    });}
    public void play(Track t){if(t!=null&&t.uri().startsWith("spotify:track:"))action("PUT","/me/player/play","{\"uris\":[\""+t.uri()+"\"]}","Playing "+t.title());}
    public void toggle(){action("PUT",paused?"/me/player/play":"/me/player/pause",null,paused?"Resumed":"Paused");}
    public void next(){action("POST","/me/player/next",null,"Next track");}
    public void previous(){action("POST","/me/player/previous",null,"Previous track");}
    public void queue(Track t){if(t!=null&&t.uri().startsWith("spotify:track:"))action("POST","/me/player/queue?uri="+enc(t.uri()),null,"Queued "+t.title());}
    public void transfer(Device device){if(device==null||device.id().isBlank())return;action("PUT","/me/player","{\"device_ids\":[\""+device.id()+"\"],\"play\":false}","Switched device");}
    /** Prefer an already-running Spotify player on a computer. Does NOT create a streaming device. */
    public void useComputer(){ executor.submit(() -> {
        try {
            fetchDevices();
            Device chosen = devices.stream().filter(d -> d.type().equalsIgnoreCase("computer") && d.active()).findFirst()
                .orElseGet(() -> devices.stream().filter(d -> d.type().equalsIgnoreCase("computer")).findFirst().orElse(null));
            if(chosen == null){status="Open Spotify on this computer and press REFRESH DEVICES";return;}
            request("PUT","/me/player","{\"device_ids\":[\""+chosen.id()+"\"],\"play\":false}");
            activeDevice=chosen.id();
            status="Computer player selected: "+chosen.name();
            fetchPlayback();
        } catch(Exception ex){status="Computer selection: "+shortError(ex);}
    }); }
    public void refreshDevices(){executor.submit(()->{try{fetchDevices();status="Devices updated";}catch(Exception ex){status="Devices: "+shortError(ex);}});}
    public void disconnect(){cancelLogin();accessToken="";refreshToken="";expireAt=0;playing="Nothing playing";tracks=List.of();devices=List.of();status="Disconnected";}
    public void shutdown(){disconnect();poller.shutdownNow();executor.shutdownNow();}
}