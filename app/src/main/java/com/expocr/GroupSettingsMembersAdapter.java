package com.expocr;

import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * adapter for recyclerview of Group members at Group settings page
 */
public class GroupSettingsMembersAdapter extends RecyclerView.Adapter<GroupSettingsMembersAdapter.MemberViewHolder> {

    private final MemberListItemClickListener mOnClickListener;
    private List<String> mData;
    private List<Integer> members_id_list;
    private static HashMap<String, Uri> members_avatar_uri_list = new HashMap<>();

    //constructor
    public GroupSettingsMembersAdapter(GroupSettingsActivity listener) {
        mOnClickListener = listener;
        mData = new ArrayList<>();
        members_id_list = new ArrayList<>();
        mData.add("Add friend to group");
        syncMemberList();
    }

    interface MemberListItemClickListener {
        void onMemberListItemClick(int clickedItemIndex);
    }

    /**
     *
     * This gets called when each new ViewHolder is created. This happens when the RecyclerView
     * is laid out. Enough ViewHolders will be created to fill the screen and allow for scrolling.
     *
     * @param viewGroup The ViewGroup that these ViewHolders are contained within.
     * @param viewType  If your RecyclerView has more than one type of item (which ours doesn't) you
     *                  can use this viewType integer to provide a different layout. See
     *                  {@link RecyclerView.Adapter#getItemViewType(int)}
     *                  for more details.
     * @return A new NumberViewHolder that holds the View for each list item
     */
    @Override
    public MemberViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        Context context = viewGroup.getContext();
        int layoutIdForListItem = R.layout.item_member_list_group_settings;
        LayoutInflater inflater = LayoutInflater.from(context);
        boolean shouldAttachToParentImmediately = false;

        View view = inflater.inflate(layoutIdForListItem, viewGroup, shouldAttachToParentImmediately);
        MemberViewHolder viewHolder = new MemberViewHolder(view);

        return viewHolder;
    }

    /**
     * OnBindViewHolder is called by the RecyclerView to display the data at the specified
     * position. In this method, we update the contents of the ViewHolder to display the correct
     * indices in the list for this particular position, using the "position" argument that is conveniently
     * passed into us.
     *
     * @param holder   The ViewHolder which should be updated to represent the contents of the
     *                 item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */

    @Override
    public void onBindViewHolder(MemberViewHolder holder, int position) {
        //populates the view with data
        holder.bind(position);
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public List<String> getmData(){
        return mData;
    }

    public List<Integer> getMembers_id_list() {
        return members_id_list;
    }

    public HashMap<String, Uri> getMembers_avatar_uri_list() {
        return members_avatar_uri_list;
    }

    /**
     * synchronize member info in the group from server, fill in the group member list in group settings page
     */
    public void syncMemberList(){
        new MembersQueryTask().execute();
    }

    //inner class
    class MemberViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        ImageView item_avatar;
        TextView item_name;
        TextView item_email;
        TextView item_balance;

        //constructor
        MemberViewHolder(View itemView){
            super(itemView);
            itemView.setOnClickListener(this);

            item_avatar = (ImageView) itemView.findViewById(R.id.item_member_list_group_avatar);
            item_name = (TextView) itemView.findViewById(R.id.item_member_list_group_name);
            item_email = (TextView) itemView.findViewById(R.id.item_member_list_group_email);
            item_balance = (TextView) itemView.findViewById(R.id.item_member_list_group_balance);
        }

        void bind(final int listIndex){

            if (listIndex == 0) {
                item_avatar.setImageResource(R.drawable.ic_add_black);
                item_name.setText(mData.get(listIndex));
                item_email.setVisibility(View.GONE);
                item_balance.setVisibility(View.GONE);
            } else {
                item_avatar.setImageURI(null);
                item_avatar.setImageResource(R.drawable.ic_uiuc_seal);
                Uri avatarUri;
                try {
                    if (!members_avatar_uri_list.isEmpty() && !members_id_list.isEmpty()) {
                        avatarUri = members_avatar_uri_list.get(String.valueOf(members_id_list.get(listIndex - 1)));
                    } else {
                        avatarUri = null;
                    }
                }
                catch(IndexOutOfBoundsException e)
                {
                    avatarUri=null;
                }
                if (avatarUri != null) {
                    item_avatar.setImageURI(null);
                    item_avatar.setImageURI(avatarUri);
                }

                String rawData = mData.get(listIndex);
                String[] rawList = rawData.split(",");
                item_name.setText(rawList[1]);
                rawList = rawList[2].split(":");
                item_email.setText(rawList[0]);
                if (rawList[1].equals("pending")) {
                    item_balance.setText("newly added" + System.getProperty("line.separator") + "delete");
                    item_balance.setTextColor(((GroupSettingsActivity)mOnClickListener).getResources().getColor(R.color.negativeRed));
                    item_balance.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            AlertDialog.Builder builder = new AlertDialog.Builder((GroupSettingsActivity)mOnClickListener);
                            builder.setTitle("Delete pending friend")
                                    .setMessage("Remove this pending friend from group?")
                                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            mData.remove(listIndex);
                                            int delete_id = members_id_list.remove(listIndex - 1);
                                            members_avatar_uri_list.remove(String.valueOf(delete_id));
                                            notifyDataSetChanged();
                                            dialog.dismiss();
                                        }
                                    })
                                    .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.cancel();
                                        }
                                    });
                            builder.create().show();
                        }
                    });
                } else {
                    BigDecimal bd = new BigDecimal(rawList[1]);
                    bd = bd.setScale(2, BigDecimal.ROUND_HALF_UP);
                    double bal = bd.doubleValue();

                    if(bal < 0){
                        item_balance.setText("owes" + System.getProperty("line.separator") + "$" + Math.abs(bal));
                        item_balance.setTextColor(((GroupSettingsActivity)mOnClickListener).getResources().getColor(R.color.negativeRed));
                    }else{
                        item_balance.setText("gets back" + System.getProperty("line.separator") + "$" + bal);
                        item_balance.setTextColor(((GroupSettingsActivity)mOnClickListener).getResources().getColor(R.color.moneyGreen));
                    }
                }
            }
        }

        @Override
        public void onClick(View v) {
            int clickedPosition = getAdapterPosition();
            mOnClickListener.onMemberListItemClick(clickedPosition);
        }
    }

    /**
     * AsyncTask to request member information from server, fill in the dataset and update recyclerview content
     */
    private class MembersQueryTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            List<GroupTransaction.Pair> list = GroupTransaction.getUserNetBalances(((GroupSettingsActivity)mOnClickListener).getG_id());
            fill_Members_list(list);

            members_avatar_uri_list.clear();
            for (int member_id: members_id_list) {
                System.out.println("member_id: " + member_id);
                members_avatar_uri_list.put(String.valueOf(member_id), FriendAdapter.download_friend_avatar(member_id));
            }

            return "";
        }

        @Override
        protected void onPostExecute(String s) {
            notifyDataSetChanged();
            Message msg = new Message();
            msg.what = GroupSettingsActivity.FINISH_MEMBERS_SYNC;
            ((GroupSettingsActivity)mOnClickListener).getHandler().sendMessage(msg);
        }
    }

    private void fill_Members_list(List<GroupTransaction.Pair> list){
        mData.clear();
        members_id_list.clear();
        mData.add("Add friend to group");//do not forget
        for (GroupTransaction.Pair pair: list) {
            StringBuilder stringBuilder = new StringBuilder().append(pair.uid).append(",")
                    .append(pair.u_name).append(",")
                    .append(pair.u_email).append(":")
                    .append(pair.amount);
            mData.add(stringBuilder.toString());
            members_id_list.add(pair.uid);
        }
    }

}
