package com.expocr;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import com.expocr.util.LoadingDialog;
import com.expocr.util.ServerUtil;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import android.os.Handler;

/**
 * An activity for the settings page of an individual group. Here, the group's name can be changed,
 * members can be added, or the group can be deleted.
 */
public class GroupSettingsActivity extends AppCompatActivity implements GroupSettingsMembersAdapter.MemberListItemClickListener {

    public static final int DELETED = 1;
    public static final int FINISH_ADD_AVATAR = 2;
    public static final int FINISH_MEMBERS_SYNC = 3;

    private int g_id;

    private EditText name_text;

    private RecyclerView membersList;
    private GroupSettingsMembersAdapter membersAdapter;
    private Handler handler;

    private Dialog loading_dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_settings);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        g_id = getIntent().getIntExtra("g_id", 1);
        name_text = (EditText) findViewById(R.id.name_group_settings);
        name_text.setText(getIntent().getStringExtra("g_name"));

        membersList = (RecyclerView) findViewById(R.id.rv_members_group_settings);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this) {
            @Override
            public boolean canScrollVertically() {
                return false;
            }
        };
        membersList.setLayoutManager(layoutManager);

        loading_dialog = LoadingDialog.showDialog(this, "Retrieving group members information...");
        membersAdapter = new GroupSettingsMembersAdapter(this);
        membersList.setAdapter(membersAdapter);

        Button delete_group_btn = (Button) findViewById(R.id.delete_group);
        delete_group_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                delete_group();
            }
        });

        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case DELETED:
                        Intent gotoMain = new Intent(GroupSettingsActivity.this, MainActivity.class);
                        startActivity(gotoMain);
                        break;
                    case FINISH_ADD_AVATAR:
                        membersAdapter.notifyDataSetChanged();
                        LoadingDialog.closeDialog(loading_dialog);
                        break;
                    case FINISH_MEMBERS_SYNC:
                        LoadingDialog.closeDialog(loading_dialog);
                        break;
                }
            }
        };
    }

    /**
     * Creates menu to show "Save" button
     * @param menu
     * @return true
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_group_settings, menu);
        return true;
    }

    /**
     * Saves the group's settings if the "Save" button is clicked
     * @param item
     * @return boolean
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_group_settings_save:
                loading_dialog = LoadingDialog.showDialog(GroupSettingsActivity.this, "Saving Group Settings...");
                saveSettings();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Provides a dropdown menu of all friends that can be added to the group when "Add Friend to Group"
     * button is clicked.
     * @param clickedItemIndex
     */
    @Override
    public void onMemberListItemClick(final int clickedItemIndex) {
        if (clickedItemIndex == 0) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            LayoutInflater inflater = getLayoutInflater();
            View new_member_view = inflater.inflate(R.layout.dialog_new_member_group_settings, null);
            final Spinner spinner_view = (Spinner) new_member_view.findViewById(R.id.dialog_new_member_group_settings_spinner);

            final List<String> spinner_list = new ArrayList<>(FriendAdapter.get_friend_brief_list());

            ArrayAdapter<String> spinner_adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, spinner_list);
            spinner_view.setAdapter(spinner_adapter);

            builder.setTitle("Add friend to group")
                    .setView(new_member_view)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener(){

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String selectedText = (String)spinner_view.getSelectedItem();
                            int leftBracket = selectedText.indexOf("(");
                            int rightBracket = selectedText.lastIndexOf(")");
                            String u_name = selectedText.substring(0, leftBracket);
                            String u_email = selectedText.substring(leftBracket + 1, rightBracket);
                            final int u_id = FriendAdapter.get_friend_id_list().get(FriendAdapter.get_friend_email_list().indexOf(u_email));
                            StringBuilder stringbuilder = new StringBuilder().append(u_id).append(",")
                                    .append(u_name).append(",")
                                    .append(u_email).append(":")
                                    .append("pending");
                            boolean isInGroup = false;
                            for (String rawData: membersAdapter.getmData()) {
                                if (rawData.contains(u_email)) {
                                    isInGroup = true;
                                    break;
                                }
                            }
                            if (!isInGroup) {//do not forget
                                loading_dialog = LoadingDialog.showDialog(GroupSettingsActivity.this, "Retrieving friend information...");
                                membersAdapter.getmData().add(clickedItemIndex + 1, stringbuilder.toString());
                                membersAdapter.getMembers_id_list().add(clickedItemIndex, u_id);//take care
                                membersAdapter.getMembers_avatar_uri_list().put(String.valueOf(u_id), null);
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        membersAdapter.getMembers_avatar_uri_list().put(String.valueOf(u_id), FriendAdapter.download_friend_avatar(u_id));
                                        Message msg = new Message();
                                        msg.what = FINISH_ADD_AVATAR;
                                        handler.sendMessage(msg);
                                    }
                                }).start();
                            }
                            dialog.dismiss();
                        }
                    })
                    .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });

            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    /**
     * Getter for group id
     * @return int g_id
     */
    public int getG_id() {
        return g_id;
    }

    public Handler getHandler() {
        return handler;
    }

    private void saveSettings(){
        new SaveQueryTask().execute();
    }

    /**
     * AsyncTask to save group name and newly added group members to server
     */
    private class SaveQueryTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            add_member_by_emails();
            return save_group_name();
        }

        @Override
        protected void onPostExecute(String s){
            LoadingDialog.closeDialog(loading_dialog);
            Intent intent = new Intent(GroupSettingsActivity.this, IndividualGroupActivity.class);
            if (s != null) {
                intent.putExtra("new_group_name", s);
            }
            NavUtils.navigateUpTo(GroupSettingsActivity.this, intent);
        }
    }

    private String save_group_name() {
        if (name_text.getText().toString().length() == 0){
            return null;
        }
        String serverUrl = "http://" + ServerUtil.getServerAddress() + "group/update_group_name";
        String requestBody = "g_id=" + g_id + "&g_name=" + name_text.getText().toString();

        String text = ServerUtil.sendData(serverUrl, requestBody, "UTF-8");

        try {
            JSONObject jsonObject = new JSONObject(text);
            if(jsonObject.has("updated_rows") && jsonObject.getInt("updated_rows") > 0) {
                return name_text.getText().toString();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    /**
     * send newly added group members to server
     */
    private void add_member_by_emails() {
        List<String> u_emails = new ArrayList<>();

        for (int index = 1; index < membersAdapter.getmData().size(); index++ ) {
            String rawData = membersAdapter.getmData().get(index);
            String[] rawList = rawData.split(",");
            rawList = rawList[2].split(":");
            String u_email = rawList[0];
            String balance = rawList[1];
            if (balance.equals("pending")) {
                u_emails.add(u_email);
            }
        }

        for (String u_email: u_emails) {
            String serverUrl = "http://" + ServerUtil.getServerAddress() + "group/add_member_by_email";
            String requestBody = "g_id=" + g_id + "&u_email=" + u_email;

            String text = ServerUtil.sendData(serverUrl, requestBody, "UTF-8");
        }
    }

    private void delete_group() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String serverUrl = "http://" + ServerUtil.getServerAddress() + "group/delete_group";
                String requestString = "id=" + g_id + "&name=" + name_text;
                String response = ServerUtil.sendData(serverUrl, requestString, "UTF-8");
                Message msg = new Message();
                msg.what = DELETED;
                handler.sendMessage(msg);
            }
        }).start();
    }

}
