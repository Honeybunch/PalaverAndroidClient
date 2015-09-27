package io.sargent.chatrooms;

import android.graphics.Color;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.Socket;
import com.github.nkzawa.socketio.client.IO;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {
    private static final String TAG = "ChatActivity";

    private Socket mSocket;
    {
        try{
            mSocket = IO.socket("https://immense-chamber-1061.herokuapp.com/");
            Log.d(TAG, "Connected");
        } catch (URISyntaxException e){
            Toast.makeText(this, R.string.connection_error, Toast.LENGTH_SHORT);
            Log.d(TAG, "Error: Unable to connect to IP. " + e.getMessage());
        }
    }

    private Toolbar mActionbar;

    private DrawerLayout mDrawer;
    private ActionBarDrawerToggle mDrawerToggle;

    private RecyclerView mMessageList;
    private LinearLayoutManager mMessagesLayoutManager;

    private ImageButton mSendMessageButton;
    private EditText mMessageText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Server socket
        mSocket.on(Socket.EVENT_ERROR, onError);
        mSocket.on(Socket.EVENT_DISCONNECT, onDisconnect);
        mSocket.on(Socket.EVENT_CONNECT_ERROR, onConnectError);
        mSocket.on(Socket.EVENT_CONNECT_TIMEOUT, onConnectError);
        mSocket.on(Socket.EVENT_RECONNECT_ATTEMPT, onReconnectAttempt);
        mSocket.on("message", onMessageRecieved);;
        mSocket.connect();

        mActionbar = (Toolbar) findViewById(R.id.tool_bar);
        setSupportActionBar(mActionbar);

        mMessageText = (EditText)findViewById(R.id.message_text);

        mMessageList = (RecyclerView)findViewById(R.id.message_view);
        mMessageList.setHasFixedSize(true);
        mMessagesLayoutManager = new LinearLayoutManager(this);
        mMessagesLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mMessageList.setLayoutManager(mMessagesLayoutManager);

        TextMessageAdapter textAdapter = new TextMessageAdapter(createList(5));
        mMessageList.setAdapter(textAdapter);

        mDrawer = (DrawerLayout)findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawer, mActionbar, R.string.open_drawer, R.string.close_drawer) {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                // code here will execute once the drawer is opened
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                // Code here will execute once drawer is closed
            }
        };

        mSendMessageButton = (ImageButton)findViewById(R.id.send_message_text);
        mSendMessageButton.setColorFilter(Color.YELLOW);
        mSendMessageButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                String m = mMessageText.getText().toString();

                if(TextUtils.isEmpty(m)){
                    return;
                } else {
                    attemptSendMessage(m);
                }
            }
        });
    }

    private List<TextMessageInfo> createList(int size){
        List<TextMessageInfo> result = new ArrayList<TextMessageInfo>();

        for(int i = 1; i <= size; i++){
            TextMessageInfo m = new TextMessageInfo("User" + i, "Testestetstestetst");

            result.add(m);
        }

        return result;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_chat, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        //mSocket.disconnect();
        //mSocket.off(Socket.EVENT_ERROR, onError);
        //mSocket.off(Socket.EVENT_CONNECT_ERROR, onConnectError);
        //mSocket.off(Socket.EVENT_CONNECT_TIMEOUT, onConnectError);
        //mSocket.off(Socket.EVENT_RECONNECT_ATTEMPT, onReconnectAttempt);
        //mSocket.off("message", onMessageRecieved);
    }

    private void attemptSendMessage(String msg){
        // format of message { "username":"xxxx", "message":"xxxx" }

        if(!mSocket.connected()){
            Log.d(TAG, "NOT CONNECTED");
        }
        String formatString = "{ \"username\":\"test\", \"message\":\"" + msg + "\" }";

        Log.d(TAG, formatString);
        mMessageText.setText("");
        mSocket.emit("messageAll", formatString);
    }

    private Emitter.Listener onMessageRecieved = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    String username;
                    String message;
                    try {
                        username = data.getString("username");
                        message = data.getString("message");
                    } catch (JSONException e) {
                        return;
                    }

                    // add the message to view
                    //addMessage(username, message);
                    Log.d(TAG, username + " " + message);
                }
            });
        }
    };

    private Emitter.Listener onDisconnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "Disconnected");
                }
            });
        }
    };

    private Emitter.Listener onError = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "Connection error");
                }
            });
        }
    };

    private Emitter.Listener onConnectError = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(),
                            R.string.connection_error, Toast.LENGTH_LONG).show();
                }
            });
        }
    };

    private Emitter.Listener onReconnectAttempt = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "Reconnect attempt");
                }
            });
        }
    };
}