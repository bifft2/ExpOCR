package com.example.mihika.expocr;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Message;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ExpenseTabAdapter extends RecyclerView.Adapter<ExpenseTabAdapter.ExpenseViewHolder> {

    //number of views it will hold
    private int mNumberItems;
    private int maxItemNumber;
    private int u_id;
    private boolean isRefreshing;

    private final ExpenseListItemClickListener mOnClickListener;
    private List<String> mData;

    //constructor
    public ExpenseTabAdapter(int numberOfItems, int u_id, TabFragment listener) {
        maxItemNumber = numberOfItems;
        this.u_id = u_id;
        mOnClickListener = listener;
        mData = new ArrayList<>();
        isRefreshing = false;
        syncExpenseList();
    }

    interface ExpenseListItemClickListener {
        void onExpenseListItemClick(int clickedItemIndex);
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
    public ExpenseViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        Context context = viewGroup.getContext();
        int layoutIdForListItem = R.layout.expense_list_item;
        LayoutInflater inflater = LayoutInflater.from(context);
        boolean shouldAttachToParentImmediately = false;

        View view = inflater.inflate(layoutIdForListItem, viewGroup, shouldAttachToParentImmediately);
        ExpenseViewHolder viewHolder = new ExpenseViewHolder(view);

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
    public void onBindViewHolder(ExpenseViewHolder holder, int position) {
        //populates the view with data
        holder.bind(position);
    }

    @Override
    public int getItemCount() {
        mNumberItems = mData.size();
        return mNumberItems;
    }

    public List<String> getmData(){
        return this.mData;
    }

    public int getU_id(){
        return this.u_id;
    }

    public void setIsRefreshing(boolean isRefreshing){
        this.isRefreshing = isRefreshing;
    }

    public void syncExpenseList(){
        new ExpensesQueryTask().execute();
    }

    //inner class
    class ExpenseViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        ImageView item_avatar;
        TextView item_info;
        TextView item_alert;
        TextView item_date;

        //constructor
        ExpenseViewHolder(View itemView){
            super(itemView);
            itemView.setOnClickListener(this);

            item_avatar = (ImageView) itemView.findViewById(R.id.expense_list_item_icon);
            item_info = (TextView) itemView.findViewById(R.id.expense_list_item_info);
            item_alert = (TextView) itemView.findViewById(R.id.expense_list_item_alert);
            item_date = (TextView) itemView.findViewById(R.id.expense_list_item_date);

        }

        void bind(int listIndex){
            String rawData = mData.get(listIndex);
            String[] rawList = rawData.split(",");
            StringBuilder builder = new StringBuilder();

            Calendar calendar = Calendar.getInstance();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
            try {
                calendar.setTime(sdf.parse(rawList[3]));
            } catch (ParseException e) {
                e.printStackTrace();
            }
            builder.append(calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()))
                    .append(" ").append(calendar.get(Calendar.DAY_OF_MONTH))
                    .append(", ").append(calendar.get(Calendar.YEAR));
            item_date.setText(builder.toString());
            builder.setLength(0);

            double amount = Double.parseDouble(rawList[2]);
            if(amount < 0){
                builder.append("You received $").append(Math.abs(amount));
                item_alert.setText(builder.toString());
                item_alert.setTextColor(((TabFragment)mOnClickListener).getResources().getColor(R.color.orange));
                builder.setLength(0);
                builder.append("You received a payment from ").append(rawList[1]);
                item_info.setText(builder.toString());
                item_avatar.setImageResource(R.drawable.ic_list_money_in);
            }else{
                builder.append("You paid $").append(amount);
                item_alert.setText(builder.toString());
                item_alert.setTextColor(((TabFragment)mOnClickListener).getResources().getColor(R.color.green));
                builder.setLength(0);
                builder.append("You made a payment to ").append(rawList[1]);
                item_info.setText(builder.toString());
                item_avatar.setImageResource(R.drawable.ic_list_money_off);
            }
            builder.setLength(0);
        }

        @Override
        public void onClick(View v) {
            int clickedPostion = getAdapterPosition();
            mOnClickListener.onExpenseListItemClick(clickedPostion);
        }
    }

    private class ExpensesQueryTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            return expense_retrieve_all();
        }

        @Override
        protected void onPostExecute(String s){
            fill_expenses_list(s);
            mNumberItems = mData.size() > maxItemNumber ? maxItemNumber : mData.size();
            if(isRefreshing){
                isRefreshing = false;//do not forget
                Message msg = new Message();
                msg.what = ((TabFragment)mOnClickListener).EXPENSE_FRAGMENT_REFRESH;
                ((TabFragment)mOnClickListener).getHandler().sendMessage(msg);
            }
        }
    }

    private void fill_expenses_list(String s){
        JSONArray jsonArray = null;
        try {
            jsonArray = new JSONArray(s);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        int limit = maxItemNumber;
        mData.clear();
        for(int index = 0; index < jsonArray.length() && index < limit; index++){
            JSONObject jsonObj = null;
            try {
                jsonObj = jsonArray.getJSONObject(index);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            StringBuilder builder = new StringBuilder();
            try {
                builder.append(jsonObj.get("receiver_id")).append(",")
                        .append(jsonObj.get("receiver_name")).append(",")
                        .append(jsonObj.get("amount")).append(",")
                        .append(jsonObj.get("date"));
                mData.add(builder.toString());
                builder.setLength(0);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        notifyDataSetChanged();
    }

    private String expense_retrieve_all(){
        String serverUrl = "http://10.0.2.2:8000/transaction/get_by_sender";
        URL url = null;
        BufferedInputStream bis = null;
        ByteArrayOutputStream baos;
        BufferedOutputStream bos = null;
        HttpURLConnection connection = null;
        byte[] responseBody = null;
        try {
            url = new URL(serverUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoInput(true);
            connection.setDoOutput(true);
            OutputStream os = connection.getOutputStream();
            String requestBody = "sender_id=" + u_id;
            os.write(requestBody.getBytes("UTF-8"));
            os.flush();
            os.close();
            InputStream is = connection.getInputStream();
            bis =  new BufferedInputStream(is);
            baos = new ByteArrayOutputStream();
            bos = new BufferedOutputStream(baos);
            byte[] response_buffer = new byte[1024];
            int length = 0;
            while((length = bis.read(response_buffer)) > 0){
                bos.write(response_buffer, 0, length);
            }
            bos.flush();
            responseBody = baos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bos != null) {
                    bos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (bis != null) {
                    bis.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                connection.disconnect();
            }
        }
        String text = null;
        try {
            text = new String(responseBody, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return text;
    }


}