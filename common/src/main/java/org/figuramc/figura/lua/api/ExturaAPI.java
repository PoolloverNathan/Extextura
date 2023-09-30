package org.figuramc.figura.lua.api;

import org.figuramc.figura.config.ConfigType;
import org.figuramc.figura.config.Configs;
import org.luaj.vm2.LuaError;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.lua.LuaNotNil;
import org.figuramc.figura.lua.LuaWhitelist;
import org.figuramc.figura.lua.docs.LuaTypeDoc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.*;
import java.util.concurrent.CompletableFuture;

import org.figuramc.figura.lua.docs.LuaMethodDoc;
import org.luaj.vm2.LuaFunction;

@LuaWhitelist
@LuaTypeDoc(
        name = "ExturaAPI",
        value = "extura"
)
public class ExturaAPI {
    private final Avatar owner;
    private final boolean isHost;
    private final static Integer VERSION = 5;

    public ExturaAPI(Avatar owner) {
        this.isHost = (this.owner = owner).isHost;
    }
    @LuaWhitelist
    @LuaMethodDoc("extura.get_figura_setting")
    public Object getFiguraSetting(String arg) {
        if (arg == null || !this.isHost) return null;
        Field obj;
        try {
             obj = Configs.class.getDeclaredField(arg);
        }catch(java.lang.NoSuchFieldException ignored){
            return null;
        }
        try {
            return ((ConfigType<?>) obj.get(null)).value;
        }catch(java.lang.IllegalAccessException ignored){
            return null;
        }
    }

    @LuaWhitelist
    @LuaMethodDoc("extura.http_get")
    public Object httpGet(String arg) {
        if (!Configs.EXPOSE_SENSITIVE_LIBRARIES.value || arg == null || (!this.isHost && !Configs.EXPOSE_HTTP.value))  return null;
        try{
            // https://docs.oracle.com/javase/tutorial/networking/urls/readingWriting.html my beloved
            URLConnection connec = new URI(arg).toURL().openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(connec.getInputStream()));
            String ret = "";
            String inLine;
            while ((inLine = in.readLine()) != null) ret += inLine;
            return ret;
        }catch (URISyntaxException | MalformedURLException err) {
            throw new LuaError("Unable to parse URL: " + err);
        }catch(IOException err){
            throw new LuaError("Unable to send request: " + err);
        }
    }
    @LuaWhitelist
    @LuaMethodDoc("extura.async_lua_function")
    public void asyncLuaFunction(LuaFunction func) {
        if (!this.isHost) return;
        CompletableFuture.runAsync(() -> {
            func.call();
        });
    }
    @LuaWhitelist
    @LuaMethodDoc("extura.async_http_get")
    public void asyncHttpGet(String arg, LuaFunction func) {
        if (!Configs.EXPOSE_SENSITIVE_LIBRARIES.value || arg == null || (!this.isHost && !Configs.EXPOSE_HTTP.value))  return;
        CompletableFuture.runAsync(() -> {
            try{
                // https://docs.oracle.com/javase/tutorial/networking/urls/readingWriting.html my beloved
                URLConnection connec = new URI(arg).toURL().openConnection();
                BufferedReader in = new BufferedReader(new InputStreamReader(connec.getInputStream()));
                String ret = "";
                String inLine;
                while ((inLine = in.readLine()) != null) ret += inLine;
                func.call(ret);
                return;
            }catch (URISyntaxException | MalformedURLException err) {
                throw new LuaError("Unable to parse URL: " + err);
            }catch(IOException err){
                throw new LuaError("Unable to send request: " + err);
            }
        });
        return;
    }
    @LuaWhitelist
    public Object __index(String arg) {
        return switch (arg) {
        	case "isHost" -> isHost;
        	case "version" -> VERSION;
            default -> null;
        };
    }

    @LuaWhitelist
    public void __newindex(@LuaNotNil String key) {
//        switch (key) {
//            default ->
        throw new LuaError("Cannot assign value on key \"" + key + "\"");
//        }
    }

    @Override
    public String toString() {
        return "ExturaAPI";
    }
}
