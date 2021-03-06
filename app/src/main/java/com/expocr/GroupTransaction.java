package com.expocr;

import com.expocr.util.ServerUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

/**
 * This class allows the app to display and work with group transactions.
 */
public class GroupTransaction {

    private double amount;
    private Calendar date;
    private String memo;
    private String category;
    private String u_name;
    private String u_email;
    private int gid;
    private int uid;

    /**
     * Gets all group transactions for a certain group and returns a List to display them with.
     * @param g_id group id
     * @return List a list of all of the group's transactions
     */
    public static List<GroupTransaction> getGroupTransactionsFromServer(int g_id){
        String serverUrl = "http://" + ServerUtil.getServerAddress() + "group/get_group_transactions";
        String requestBody = "g_id="+g_id;

        String text = ServerUtil.sendData(serverUrl, requestBody, "UTF-8");

        System.out.println("getGroupTransactionsFromServer: " + text);

        List<GroupTransaction> retval = new ArrayList<>();
        try {
            JSONArray jsonArray = new JSONArray(text);
            for (int index = 0; index < jsonArray.length(); index++) {
                JSONObject jsonObject = jsonArray.getJSONObject(index);
                GroupTransaction groupTransaction = new GroupTransaction();
                groupTransaction.gid = g_id;
                groupTransaction.uid = jsonObject.getInt("u_id");
                groupTransaction.amount = jsonObject.getDouble("amount");
                groupTransaction.u_name = jsonObject.getString("u_name");
                groupTransaction.u_email = jsonObject.getString("u_email");
                retval.add(groupTransaction);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return retval;
    }

    /**
     * Gets all pairs where one user owes another user in the group.
     * @param g_id group id
     * @param u_id user id
     * @return List a list of pairs of people who owe each other in the group
     */
    public static List<Pair> getOwedAmounts(int g_id, int u_id){
        List<Pair> amounts = getUserNetBalances(g_id);
        List<Pair> dues = new ArrayList<>();

        List<Pair> positives = new ArrayList<>();
        List<Pair> negatives = new ArrayList<>();
        for(Pair pair : amounts){
            if(pair.amount > 0)
                positives.add(pair);
            else if(pair.amount < 0)
                negatives.add(pair);
        }

        System.out.println("getOwedAmounts: begins while");
        int posIdx = 0;
        int negIdx = 0;
        while (posIdx < positives.size() && negIdx < negatives.size()){
            if (positives.get(posIdx).uid == u_id){
                if(-negatives.get(negIdx).amount > positives.get(posIdx).amount){
                    dues.add(new Pair(negatives.get(negIdx).uid,positives.get(posIdx).amount, negatives.get(negIdx).u_name, negatives.get(negIdx).u_email));
                    break;
                } else if(-negatives.get(negIdx).amount == positives.get(posIdx).amount){
                    dues.add(new Pair(negatives.get(negIdx).uid,positives.get(posIdx).amount, negatives.get(negIdx).u_name, negatives.get(negIdx).u_email));
                    break;
                }
                else{
                    positives.get(posIdx).amount += negatives.get(negIdx).amount;
                    dues.add(new Pair(negatives.get(negIdx).uid,-negatives.get(negIdx).amount, negatives.get(negIdx).u_name, negatives.get(negIdx).u_email));
                    negIdx++;
                }
            }
            else if (negatives.get(negIdx).uid == u_id){
                if(positives.get(posIdx).amount > -negatives.get(negIdx).amount){
                    dues.add(new Pair(positives.get(posIdx).uid,negatives.get(negIdx).amount, positives.get(posIdx).u_name, positives.get(posIdx).u_email));
                    break;
                } else if(-negatives.get(negIdx).amount == positives.get(posIdx).amount){
                    dues.add(new Pair(positives.get(posIdx).uid,negatives.get(negIdx).amount, positives.get(posIdx).u_name, positives.get(posIdx).u_email));
                    break;
                }
                else{
                    negatives.get(negIdx).amount += positives.get(posIdx).amount;
                    dues.add(new Pair(positives.get(posIdx).uid,-positives.get(posIdx).amount, positives.get(posIdx).u_name, positives.get(posIdx).u_email));
                    posIdx++;
                }
            }
            else{
                if(-negatives.get(negIdx).amount > positives.get(posIdx).amount){
                    negatives.get(negIdx).amount += positives.get(posIdx).amount;
                    posIdx++;
                } else if(-negatives.get(negIdx).amount == positives.get(posIdx).amount){
                    negIdx++;
                    posIdx++;
                }
                else{
                    positives.get(posIdx).amount += negatives.get(negIdx).amount;
                    negIdx++;
                }
            }
        }
        System.out.println("getOwedAmounts: ends while");

        return dues;
    }

    /**
     * Gets the user's net balance for the specific group
     * @param g_id group id
     * @param u_id user id
     * @return double net balance of user for the group
     */
    public static double getUserNetBalance(int g_id, int u_id){
        List<GroupTransaction> mDataSource = getGroupTransactionsFromServer(g_id);
        HashMap<Integer, Double> balances= new HashMap<>();
        for(GroupTransaction x : mDataSource) {
            if (balances.containsKey(x.uid)) {
                balances.put(x.uid, x.amount + balances.get(x.uid));
            }
            else
                balances.put(x.uid, x.amount);
        }

        return balances.isEmpty() || !balances.containsKey(u_id) ? 0: balances.get(u_id);

    }

    /**
     * Gets a list of net balance for each member of the group.
     * @param g_id group id
     * @return List a list of pairs (members and their net balances)
     */
    public static List<Pair> getUserNetBalances(int g_id){
        List<GroupTransaction> mDataSource = getGroupTransactionsFromServer(g_id);
        HashMap<Integer, GroupTransaction> balances= new HashMap<>();
        for(GroupTransaction x : mDataSource) {
            if (balances.containsKey(x.uid)) {
                x.amount += balances.get(x.uid).amount;
                balances.put(x.uid, x);
            }
            else
                balances.put(x.uid, x);
        }

        List<Pair> pairs = new ArrayList<>();
        for(Object uid : balances.keySet().toArray()){
            GroupTransaction value = balances.get(uid);
            pairs.add(new Pair(value.uid, value.amount, value.u_name, value.u_email));
        }

        List<Integer> members = getMembers(g_id);
        List<Integer> missing_members = new ArrayList<>();
        for (int m_id: members) {
            if (!balances.containsKey(m_id)) {
                missing_members.add(m_id);
            }
        }
        if(!missing_members.isEmpty()) {
            List<Pair> missing_member_briefs = getMemberBriefs(missing_members);
            pairs.addAll(missing_member_briefs);
        }

        return pairs;
    }

    /**
     *
     * @param mids
     * @return
     */
    public static List<Pair> getMemberBriefs(List<Integer> mids) {
        String serverUrl = "http://" + ServerUtil.getServerAddress() + "user/get_briefs";
        StringBuilder stringBuilder = new StringBuilder();
        for(int mid: mids) {
            stringBuilder.append(mid).append(",");
        }
        stringBuilder.deleteCharAt(stringBuilder.length() - 1);

        String requestBody = "u_ids=" + stringBuilder.toString();

        String text = ServerUtil.sendData(serverUrl, requestBody, "UTF-8");

        List<Pair> members = new ArrayList<>();
        try {
            JSONArray jsonArray = new JSONArray(text);
            for(int index = 0; index < jsonArray.length(); index++) {
                JSONObject jsonObject = jsonArray.getJSONObject(index);
                members.add(new Pair(jsonObject.getInt("u_id"), 0.0, jsonObject.getString("u_name"), jsonObject.getString("email")));
            }
        } catch (JSONException jsonex) {
            jsonex.printStackTrace();
        }

        return members;
    }

    public static List<Integer> getMembers(int g_id) {
        String serverUrl = "http://" + ServerUtil.getServerAddress() + "group/get_members";
        String requestBody = "g_id="+g_id;

        String text = ServerUtil.sendData(serverUrl, requestBody, "UTF-8");

        List<Integer> members = new ArrayList<>();
        try {
            JSONArray jsonArray = new JSONArray(text);
            for(int index = 0; index < jsonArray.length(); index++) {
                JSONObject jsonObject = jsonArray.getJSONObject(index);
                members.add(jsonObject.getInt("u_id"));
            }
        } catch (JSONException jsonex) {
            jsonex.printStackTrace();
        }

        return members;
    }

    /**
     * stucture class to hold a single group member information
     */
    static class Pair {
        public Integer uid;
        public Double amount;
        public String u_name;
        public String u_email;

        Pair(Integer uid, Double amount, String u_name, String u_email){
            this.uid = uid;
            this.amount = amount;
            this.u_name = u_name;
            this.u_email = u_email;
        }

        @Override
        public String toString() {
            return "uid: " + uid + ", amount: " + amount;
        }

        public String getUserBrief() {
            return u_name + " (" + u_email + ")";
        }

    }

    public static void main(String[] args){
        System.out.println("running");
        System.out.println("part 1");
        for (int i = 1; i<=6; i++){
            List<Pair> retval = GroupTransaction.getOwedAmounts(1,i);
            for (Pair p : retval){
                System.out.println("group 1 uid "+i);
                System.out.println(p.uid+" "+p.amount);
            }
        }
        System.out.println("part 2");
        for (int i = 7; i<=10; i++){
            List<Pair> retval = GroupTransaction.getOwedAmounts(2,i);
            for (Pair p : retval){
                System.out.println("group 2 uid "+i);
                System.out.println(p.uid+" "+p.amount);
            }
        }

    }
}