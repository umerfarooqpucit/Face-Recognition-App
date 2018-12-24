package com.example.umerfarooq.facedetection2;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;



public class MessageDialog {

    private Context context;
    private String personName;

    public MessageDialog(Context context) {
        this.context = context;
    }

    public void addNewMessage(int dialog_layout){
        LayoutInflater inflater = LayoutInflater.from(context);
        View subView = inflater.inflate(dialog_layout, null);

        final EditText nameField = (EditText)subView.findViewById(R.id.enter_message);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Enter name of person");
        builder.setView(subView);
        builder.create();
        builder.setPositiveButton("ADD PERSON", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                final String message = nameField.getText().toString();
                if(TextUtils.isEmpty(message)){
                    Toast.makeText(context, "Empty or invalid input", Toast.LENGTH_LONG).show();
                }
                else{
                    personName=message;
                }
            }
        });
        builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(context, "Task cancelled", Toast.LENGTH_LONG).show();
            }
        });
        builder.show();
    }

    public String getEnteredText() {
        return personName;
    }
}
