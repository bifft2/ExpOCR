package com.expocr;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * used to be adapter for listview in individual friend and group page
 */
public class ExpenseAdapter extends BaseAdapter {

    private Context mContext;
    private LayoutInflater mInflater;
    private ArrayList<Expense> mDataSource;

    public ExpenseAdapter(Context context, ArrayList<Expense> items) {
        mContext = context;
        mDataSource = items;
        mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return mDataSource.size();
    }

    @Override
    public Object getItem(int position) {
        return mDataSource.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get view for row item
        View rowView = mInflater.inflate(R.layout.list_item_expense, parent, false);

        TextView expenseTextView = (TextView)rowView.findViewById(R.id.expense_list_expense);
        TextView balanceTextView = (TextView)rowView.findViewById(R.id.expense_list_balance);
        TextView dayTextView = (TextView)rowView.findViewById(R.id.expense_list_day);
        TextView monthTextView = (TextView)rowView.findViewById(R.id.expense_list_month);
        TextView yearTextView = (TextView) rowView.findViewById(R.id.expense_list_year);

        Expense expense = (Expense) getItem(position);
        DecimalFormat df = new DecimalFormat("#.00");

        expenseTextView.setText(expense.getExpense());
        if(expense.getBalance() < 0) {
            double balance = expense.getBalance() * (-1);
            balanceTextView.setText("- $".concat(df.format(balance)));
            balanceTextView.setTextColor(mContext.getResources().getColor(R.color.negativeRed));
        }
        else {
            balanceTextView.setText("$".concat(df.format(expense.getBalance())));
            balanceTextView.setTextColor(mContext.getResources().getColor(R.color.moneyGreen));
        }
        dayTextView.setText(Integer.toString(expense.getDate().get(Calendar.DAY_OF_MONTH)));
        monthTextView.setText(expense.getDate().getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()));
        yearTextView.setText(Integer.toString(expense.getDate().get(Calendar.YEAR)));

        return rowView;
    }

    public List<Expense> getData(){
        return this.mDataSource;
    }

    public double getNetBalance() {
        double netBalance = 0.0;
        for(Expense x : mDataSource) {
            netBalance += x.getBalance();
        }
        return netBalance;
    }

}
