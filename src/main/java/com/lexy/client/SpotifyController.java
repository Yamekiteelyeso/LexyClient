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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
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
    private volatile boolean manualOnly=false;
    private final AtomicBoolean exchanging = new AtomicBoolean();
    private final AtomicInteger authGeneration = new AtomicInteger();
    private volatile long loginStartedAt=0, nextPollAllowed=0;
    private int pollCount=0;
    public record Track(String title,String artist,String uri) {}
    public record Device(String id,String name,String type,boolean active) {}
    private SpotifyController(){poller.scheduleWithFixedDelay(()->{
        if(isConnected() && System.currentTimeMillis()>=nextPollAllowed) executor.submit(()->{
            try {
                fetchPlayback();
                if(++pollCount%3==0) fetchDevices();
            } catch(Exception e){status="Spotify: "+shortError(e);}
        });
    },3,20,TimeUnit.SECONDS);}
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
    public boolean pending(){return !pendingAuthUrl.isBlank();}
    public boolean manualOnly(){return manualOnly;}
    public boolean busy(){return exchanging.get();}
    public void openPlayer(){
        if(AndroidBrowser.open("https://open.spotify.com/")) status="Open Spotify on phone, play a song, then tap REFRESH";
        else status="Open the Spotify app and start a song, then tap REFRESH";
    }
    public void completeFromClipboard(){
        try {
            MinecraftClient mc=MinecraftClient.getInstance();
            if(mc==null){status="Clipboard unavailable";return;}
            completeFromRedirect(mc.keyboard.getClipboard());
        }catch(Throwable ex){status="Could not read clipboard; paste callback URL manually";}
    }
    public void refresh(){executor.execute(()->{
        try{fetchPlayback();fetchDevices();status="Playback and devices updated";}
        catch(Exception ex){status="Spotify: "+shortError(ex);}
    });}
    public String redirectUri(){return REDIRECT;}
    public void cancelLogin(){authGeneration.incrementAndGet();stopCallback();pendingAuthUrl="";verifier="";state="";loginStartedAt=0;manualOnly=false;exchanging.set(false);status="Authorization cancelled";}
    public void copyAuthUrl(){
        if(pendingAuthUrl.isBlank()){status="Click LOGIN first";return;}
        try {MinecraftClient.getInstance().keyboard.setClipboard(pendingAuthUrl);status="Spotify login URL copied";}
        catch(Throwable error){status="Clipboard unavailable on this launcher";}
    }
    private String randomString(int length){byte[] bytes=new byte[length];random.nextBytes(bytes);return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);}
    private static String enc(String s){return URLEncoder.encode(s,StandardCharsets.UTF_8);}
    private static String shortError(Exception ex){String m=ex.getMessage();return m==null?ex.getClass().getSimpleName():m.substring(0,Math.min(120,m.length()));}
    public void connect(){
        if(clientId.isBlank()){status="Enter your Spotify Client ID from developer.spotify.com/dashboard";return;}
        if(!clientId.matches("[A-Za-z0-9]{20,80}")){status="Invalid Client ID. Use the public Spotify app ID, not a secret";return;}
        if(pending()){status="Login already in progress. COPY URL or paste callback.";return;}
        exchanging.set(false);
        try {
            verifier=randomString(48);state=randomString(24);loginStartedAt=System.currentTimeMillis();
            byte[] digest=MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII));
            String challenge=Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
            try {
                HttpServer server=HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"),43871),0);
                server.createContext("/callback",this::handleCallback);
                server.setExecutor(null);server.start();callback=server;manualOnly=false;
            } catch(IOException localError){
                // Pojav/Android environments can block loopback listeners; the user can paste the redirect URL.
                callback=null;manualOnly=true;
            }
            pendingAuthUrl="https://accounts.spotify.com/authorize?response_type=code&client_id="+enc(clientId)+"&redirect_uri="+enc(REDIRECT)+"&scope="+enc(SCOPES)+"&state="+enc(state)+"&code_challenge_method=S256&code_challenge="+enc(challenge);
            boolean launched=AndroidBrowser.open(pendingAuthUrl);
            if(!launched) copyAuthUrl();
            status=manualOnly ? "Pojav: after Spotify login, paste callback URL below" :
                    (launched ? "Browser launched. Approve Spotify; if needed paste callback." : "Copy the login URL, open in your mobile browser.");
        }catch(Exception ex){cancelLogin();status="Login setup failed: "+shortError(ex);}
    }
    /** Manual mobile fallback: paste the FULL redirect URL, never the account password. */
    public void completeFromRedirect(String pasted){
        if(!pending()){status="Press LOGIN first, then authorize in browser";return;}
        if(System.currentTimeMillis()-loginStartedAt>300_000L){cancelLogin();status="Login expired after 5 minutes. Press LOGIN again";return;}
        String raw=pasted==null?"":pasted.trim();
        if(raw.length()>4096 || raw.length()<20){status="Paste the full http://127.0.0.1:43871/callback?... URL";return;}
        final Map<String,String> params;
        try {
            URI uri=URI.create(raw);
            if(!"http".equalsIgnoreCase(uri.getScheme()) || !"127.0.0.1".equals(uri.getHost())
                    || uri.getPort()!=43871 || !"/callback".equals(uri.getPath())) {
                status="Wrong redirect URL. Expected 127.0.0.1:43871/callback";return;
            }
            params=query(uri.getRawQuery());
        } catch(Exception invalid){status="Invalid Spotify callback URL";return;}
        if(!state.equals(params.get("state"))){status="Security check failed: state mismatch. Restart login.";return;}
        if(params.containsKey("error")){status="Spotify rejected authorization: "+params.get("error");return;}
        String code=params.get("code");
        if(code==null||code.isBlank()){status="Callback has no authorization code";return;}
        beginTokenExchange(code);
    }
    private static Map<String,String> query(String raw){
        HashMap<String,String> out=new HashMap<>();
        if(raw!=null)for(String p:raw.split("&")){String[] a=p.split("=",2);if(a.length==2)out.put(URLDecoder.decode(a[0],StandardCharsets.UTF_8),URLDecoder.decode(a[1],StandardCharsets.UTF_8));}
        return out;
    }
    /** The browser callback must complete before Android suspends/resumes the game. */
    private void handleCallback(HttpExchange exchange){
        String message="Lexy Client received the Spotify login. Return to Minecraft and check the status.";
        String code=null;
        int statusCode=200;
        try {
            if(!"GET".equals(exchange.getRequestMethod())) {statusCode=405;message="Invalid HTTP method";}
            else {
                Map<String,String> p=query(exchange.getRequestURI().getRawQuery());
                if(!pending() || !Objects.equals(p.get("state"),state)) {statusCode=400;message="Security state mismatch. Restart login.";}
                else if(p.containsKey("error")) {statusCode=400;message="Spotify login denied. Return to Minecraft.";status="Spotify login denied";}
                else if(!p.containsKey("code")) {statusCode=400;message="Spotify callback missing authorization code";}
                else code=p.get("code");
            }
            byte[] bytes=("<html><meta name='viewport' content='width=device-width,initial-scale=1'>"+
                "<body style='font-family:system-ui;background:#101721;color:#eaf2ff;padding:30px'>"+
                "<h2>LexyClient / Spotify</h2><p>"+message+"</p>"+
                "<p>You can return to Minecraft now.</p></body></html>").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type","text/html; charset=utf-8");
            exchange.getResponseHeaders().add("Cache-Control","no-store");
            exchange.sendResponseHeaders(statusCode,bytes.length);
            exchange.getResponseBody().write(bytes);
        }catch(Exception ex){status="Spotify callback error";}
        finally {exchange.close();}
        if(code!=null) beginTokenExchange(code);
    }
    private void beginTokenExchange(String code){
        if(!pending() || !exchanging.compareAndSet(false,true)) return;
        String expectedVerifier=verifier, expectedClientId=clientId;
        int generation=authGeneration.get();
        status="Authorizing Spotify account...";
        // Exchange on the network thread, never inside the browser callback/GUI thread.
        executor.execute(()->{
            try {
                JsonObject token=tokenRequest("grant_type=authorization_code&code="+enc(code)+
                    "&redirect_uri="+enc(REDIRECT)+"&client_id="+enc(expectedClientId)+
                    "&code_verifier="+enc(expectedVerifier));
                if(generation!=authGeneration.get()) return;
                applyTokens(token);
                status="Spotify connected. Open the Spotify app and play a song";
                nextPollAllowed=0;
                try{fetchDevices();}catch(Exception ignored){}
                MinecraftClient mc=MinecraftClient.getInstance();
                if(mc!=null)mc.execute(LexySounds::success);
            }catch(Exception ex){if(generation==authGeneration.get())status="Spotify login failed: "+shortError(ex)+". Press LOGIN to retry";}
            finally{if(generation==authGeneration.get()){stopCallback();pendingAuthUrl="";verifier="";state="";loginStartedAt=0;exchanging.set(false);}}
        });
    }
    private void stopCallback(){HttpServer old=callback;callback=null;if(old!=null)old.stop(0);}
    private JsonObject tokenRequest(String form) throws Exception {
        HttpRequest req=HttpRequest.newBuilder(URI.create("https://accounts.spotify.com/api/token"))
            .header("Content-Type","application/x-www-form-urlencoded").timeout(Duration.ofSeconds(12))
            .POST(HttpRequest.BodyPublishers.ofString(form)).build();
        HttpResponse<String> resp=http.send(req,HttpResponse.BodyHandlers.ofString());
        if(resp.statusCode()!=200){
            int code=resp.statusCode();
            if(code==400)throw new IOException("Code expired, invalid Client ID, or wrong redirect URI (400)");
            if(code==401)throw new IOException("Spotify rejected your Client ID (401)");
            if(code==429)throw new IOException("Spotify rate limit; retry later (429)");
            throw new IOException("Spotify OAuth HTTP "+code);
        }
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
        if(r.statusCode()==429){
            long seconds=30;
            try{seconds=Math.max(10,Math.min(300,Long.parseLong(r.headers().firstValue("Retry-After").orElse("30"))));}
            catch(NumberFormatException ignored){}
            nextPollAllowed=System.currentTimeMillis()+seconds*1000;
            throw new IOException("Spotify rate limit. Try again in "+seconds+"s (429)");
        }
        if(r.statusCode()==401)throw new IOException("Session expired; disconnect and log in again (401)");
        if(r.statusCode()==403)throw new IOException("403: Spotify Premium/app owner + user allowlist required");
        if(r.statusCode()==404)throw new IOException("No Spotify player: start a song on phone (404)");
        if(r.statusCode()/100!=2)throw new IOException("Spotify HTTP "+r.statusCode());
        return r;
    }
    private static String get(JsonObject o,String key){return o!=null&&o.has(key)&&!o.get(key).isJsonNull()?o.get(key).getAsString():"";}
    private void fetchPlayback() throws Exception {
        HttpResponse<String> r=request("GET","/me/player",null);
        if(r.statusCode()==204||r.body().isBlank()){playing="No active player - open Spotify on phone";paused=true;activeDevice="";return;}
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
        if(!root.has("devices") || root.get("devices").isJsonNull()) {devices=List.of();return;}
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
    public void usePhone(){executor.submit(()->{
        try{
            fetchDevices();
            Device chosen=devices.stream().filter(d->d.type().equalsIgnoreCase("smartphone") || d.type().equalsIgnoreCase("tablet"))
                .filter(Device::active).findFirst().orElseGet(()->devices.stream()
                    .filter(d->d.type().equalsIgnoreCase("smartphone") || d.type().equalsIgnoreCase("tablet"))
                    .findFirst().orElse(null));
            if(chosen==null){status="Start Spotify on your phone and refresh devices";return;}
            request("PUT","/me/player","{\"device_ids\":[\""+chosen.id()+"\"],\"play\":false}");
            activeDevice=chosen.id();status="Using phone: "+chosen.name();fetchPlayback();
        }catch(Exception ex){status="Select phone: "+shortError(ex);}
    });}
    public void refreshDevices(){executor.submit(()->{try{fetchDevices();status="Devices updated";}catch(Exception ex){status="Devices: "+shortError(ex);}});}
    public void disconnect(){cancelLogin();accessToken="";refreshToken="";expireAt=0;playing="Nothing playing";tracks=List.of();devices=List.of();status="Disconnected";}
    public void shutdown(){disconnect();poller.shutdownNow();executor.shutdownNow();}
}