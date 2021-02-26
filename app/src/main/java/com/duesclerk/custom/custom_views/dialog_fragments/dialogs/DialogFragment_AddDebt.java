package com.duesclerk.custom.custom_views.dialog_fragments.dialogs;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkError;
import com.android.volley.Request;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.duesclerk.R;
import com.duesclerk.custom.custom_utilities.application.ApplicationClass;
import com.duesclerk.custom.custom_utilities.application.BroadCastUtils;
import com.duesclerk.custom.custom_utilities.application.ViewsUtils;
import com.duesclerk.custom.custom_utilities.application.VolleyUtils;
import com.duesclerk.custom.custom_utilities.user_data.ContactUtils;
import com.duesclerk.custom.custom_utilities.user_data.DataUtils;
import com.duesclerk.custom.custom_utilities.user_data.DateTimeUtils;
import com.duesclerk.custom.custom_utilities.user_data.DebtUtils;
import com.duesclerk.custom.custom_utilities.user_data.InputFiltersUtils;
import com.duesclerk.custom.custom_utilities.user_data.UserAccountUtils;
import com.duesclerk.custom.custom_views.toast.CustomToast;
import com.duesclerk.custom.network.InternetConnectivity;
import com.duesclerk.custom.network.NetworkTags;
import com.duesclerk.custom.network.NetworkUrls;
import com.duesclerk.custom.storage_adapters.UserDatabase;
import com.duesclerk.interfaces.Interface_DatePicker;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class DialogFragment_AddDebt extends DialogFragment implements Interface_DatePicker {

    // Get class simple name
    //private final String TAG = DialogFragment_AddDebt.class.getSimpleName();

    private final LayoutInflater inflater;
    private final Context mContext;
    private final String contactFullName, contactId;
    private EditText editDebtAmount, editDebtDescription;
    private EditText editDebtDateIssuedFull, editDebtDateDueFull;
    private String shortDateDebtIssued, shortDateDebtDue;
    private ProgressDialog progressDialog;

    /**
     * Class constructor
     */
    public DialogFragment_AddDebt(final Context context, final String contactId,
                                  final String contactFullName) {

        this.mContext = context;
        this.inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.contactId = contactId;
        this.contactFullName = contactFullName;
    }

    @Override
    public @NotNull Dialog onCreateDialog(Bundle savedInstanceState) {

        final Dialog dialogAddContact = super.onCreateDialog(savedInstanceState);

        @SuppressLint("InflateParams")
        View dialogView = inflater.inflate(
                R.layout.dialog_add_debt, null, false);

        // EditTexts
        this.editDebtAmount = dialogView.findViewById(R.id.editAddDebt_DebtAmount);
        this.editDebtDateIssuedFull = dialogView.findViewById(R.id.editAddDebt_DateIssued);
        this.editDebtDateDueFull = dialogView.findViewById(R.id.editAddDebt_DateDue);
        this.editDebtDescription = dialogView.findViewById(R.id.editAddDebt_DebtDescription);

        // LinearLayouts
        LinearLayout llCancel = dialogView.findViewById(R.id.llAddDebt_Cancel);
        LinearLayout llAddDebt = dialogView.findViewById(R.id.llAddDebt_Add);

        // Initialize ProgressDialog
        this.progressDialog = ViewsUtils.initProgressDialog(requireActivity(), false);

        UserDatabase database = new UserDatabase(mContext); // Initialize user database object

        DialogFragment_DatePicker datePickerDateIssued = new DialogFragment_DatePicker(
                this, true);
        datePickerDateIssued.setRetainInstance(true);
        datePickerDateIssued.setCancelable(false);

        DialogFragment_DatePicker datePickerDateDue = new DialogFragment_DatePicker(
                this, false);
        datePickerDateDue.setRetainInstance(true);
        datePickerDateDue.setCancelable(false);

        // Cancel onClick
        llCancel.setOnClickListener(v -> dismiss()); // Dismiss dialog

        // Add person onClick
        llAddDebt.setOnClickListener(v -> {

            // Check fields
            if (checkFieldInputs()) {
                // Fields ok

                String userId = database.getUserAccountInfo(null).get(0).getUserId();

                // Add/Upload  contact
                this.addContactsDebt(
                        userId, contactId, contactFullName,
                        editDebtAmount.getText().toString(),
                        editDebtDateIssuedFull.getText().toString(),
                        editDebtDateDueFull.getText().toString(),
                        editDebtDescription.getText().toString()
                );

            }
        });

        // Date issued onClick
        this.editDebtDateIssuedFull.setOnClickListener(v -> {

            // Show add debt dialog
            ViewsUtils.showDialogFragment(getParentFragmentManager(),
                    datePickerDateIssued, true);
        });

        // Date due onClick
        this.editDebtDateDueFull.setOnClickListener(v -> {

            // Show add debt dialog
            ViewsUtils.showDialogFragment(getParentFragmentManager(),
                    datePickerDateDue, true);
        });

        // Remove window title
        dialogAddContact.getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        // Set width to match parent and height to wrap content
        Window window = dialogAddContact.getWindow();
        window.setLayout(RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);

        // Set dialog transparent background
        dialogAddContact.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialogAddContact.setContentView(dialogView);

        return dialogAddContact;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {

            // Get dialog inputs from savedInstanceState
            String savedDebtAmount = savedInstanceState
                    .getString(DebtUtils.FIELD_DEBT_AMOUNT);
            String savedDebtDateIssued = savedInstanceState
                    .getString(DebtUtils.FIELD_DEBT_DATE_ISSUED);
            String savedDebtDateDue = savedInstanceState
                    .getString(DebtUtils.FIELD_DEBT_DATE_DUE);
            String savedDebtDescription = savedInstanceState
                    .getString(DebtUtils.FIELD_DEBT_DESCRIPTION);

            // Check for values and set to EditTexts
            if (!DataUtils.isEmptyString(savedDebtAmount)) {

                editDebtAmount.setText(savedDebtAmount); // Set debt amount
            }

            // Check for values and set to EditTexts
            if (!DataUtils.isEmptyString(savedDebtDateIssued)) {

                editDebtDateIssuedFull.setText(savedDebtDateIssued); // Set debt date issued
            }

            // Check for values and set to EditTexts
            if (!DataUtils.isEmptyString(savedDebtDateDue)) {

                editDebtDateDueFull.setText(savedDebtDateDue); // Set debt date due
            }

            // Check for values and set to EditTexts
            if (!DataUtils.isEmptyString(savedDebtDescription)) {

                editDebtDescription.setText(savedDebtDescription); // Set debt description
            }
        }
    }

    @Override
    public void onSaveInstanceState(@NotNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save dialog inputs to outState

        // Check for field values and set to outState
        if (!DataUtils.isEmptyEditText(editDebtAmount)) {

            // Get and put debt amount
            outState.putString(DebtUtils.FIELD_DEBT_AMOUNT,
                    editDebtAmount.getText().toString());
        }

        // Check for field values and set to outState
        if (!DataUtils.isEmptyEditText(editDebtDateIssuedFull)) {

            // Get and put debt date issued
            outState.putString(DebtUtils.FIELD_DEBT_DATE_ISSUED,
                    editDebtDateIssuedFull.getText().toString());
        }

        // Check for field values and set to outState
        if (!DataUtils.isEmptyEditText(editDebtDateDueFull)) {

            // Get and put debt date due
            outState.putString(DebtUtils.FIELD_DEBT_DATE_DUE,
                    editDebtDateDueFull.getText().toString());
        }

        // Check for field values and set to outState
        if (!DataUtils.isEmptyEditText(editDebtDescription)) {

            // Get and put debt description
            outState.putString(DebtUtils.FIELD_DEBT_DESCRIPTION,
                    editDebtDescription.getText().toString());
        }
    }


    /**
     * Function to check field lengths and values and notify by toast on error
     */
    private boolean checkFieldInputs() {

        return (InputFiltersUtils.checkDebtAmountLengthNotify(mContext, editDebtAmount)
                && InputFiltersUtils.checkDebtDateIssuedNotify(mContext, editDebtDateIssuedFull)
                && InputFiltersUtils.checkDebtDateDueNotify(mContext, editDebtDateDueFull)
                && !dateDifferenceLessThanZero()
        );
    }

    /**
     * Function to check if date difference if greater or equal to zero
     * This prevents a negative time difference or backdating date due past date issued
     */
    private boolean dateDifferenceLessThanZero() {


        DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.SHORT);

        try {

            // Convert
            Date dateDebtIssued = dateFormat.parse(shortDateDebtIssued);
            Date dateDebtDue = dateFormat.parse(shortDateDebtDue);

            // Check if dates are null
            if ((dateDebtIssued != null) && (dateDebtDue != null)) {

                // Check if time difference is less than zero
                if (DateTimeUtils.getDateTimeDifferenceInDays(
                        dateDebtIssued, dateDebtDue) < 0) {

                    // Toast error message
                    CustomToast.errorMessage(mContext, DataUtils.getStringResource(mContext,
                            R.string.error_debt_date_due_behind_date_issued),
                            R.drawable.ic_baseline_date_range_24_white);

                    // Enable error icon
                    editDebtDateDueFull.setError(DataUtils.getStringResource(mContext,
                            R.string.error_debt_check_range));

                } else {

                    return false; // Return false - Time difference is not less than zero
                }
            }
        } catch (ParseException ignored) {
        }

        return true; // Return status
    }

    /**
     * Function to add contact to remote database
     *
     * @param userId          - Users id
     * @param contactId       - Contact id
     * @param contactFullName - Contact full name
     * @param debtAmount      - debt amount
     */
    private void addContactsDebt(final String userId, final String contactId, final String
            contactFullName, final String debtAmount, final String debtDateIssued, final String
                                         debtDateDue, final String debtDescription) {

        // Check Internet Connection states
        if (InternetConnectivity.isConnectedToAnyNetwork(mContext)) {
            // Connected

            ViewsUtils.hideKeyboard(requireActivity()); // Hide keyboard if showing

            // Show dialog
            ViewsUtils.showProgressDialog(progressDialog,
                    DataUtils.getStringResource(mContext,
                            R.string.title_adding_contact),
                    DataUtils.getStringResource(mContext,
                            R.string.msg_adding_debt, contactFullName)
            );

            StringRequest stringRequest = new StringRequest(Request.Method.POST,
                    NetworkUrls.DebtsURLS.URL_ADD_CONTACTS_DEBT, response -> {

                // Log Response
                //Log.d(TAG, "Add contacts debt response:" + response);

                ViewsUtils.dismissProgressDialog(progressDialog); // Hide Dialog

                try {

                    JSONObject jsonObject = new JSONObject(response);
                    boolean error = jsonObject.getBoolean(VolleyUtils.KEY_ERROR);

                    // Check for error
                    if (!error) {
                        // New contact added successfully

                        // Show success message
                        CustomToast.infoMessage(mContext,
                                DataUtils.getStringResource(mContext,
                                        R.string.msg_debt_adding_successful, debtAmount, contactFullName),
                                false, R.drawable.ic_baseline_attach_money_24_white);

                        try {

                            // Broadcast to refresh debts
                            Intent intentBroadcastDebts = new Intent(
                                    BroadCastUtils.bcrActionReloadContactDetailsAndDebtsActivity);

                            // Broadcast to refresh contacts
                            Intent intentBroadcastPeopleOwingMe = new Intent(
                                    BroadCastUtils.bcrActionReloadPeopleOwingMe);

                            // Broadcast to refresh contacts
                            Intent intentBroadcastPeopleIOwe = new Intent(
                                    BroadCastUtils.bcrActionReloadPeopleIOwe);

                            // Send broadcasts
                            requireActivity().sendBroadcast(intentBroadcastDebts);
                            requireActivity().sendBroadcast(intentBroadcastPeopleOwingMe);
                            requireActivity().sendBroadcast(intentBroadcastPeopleIOwe);

                        } finally {

                            dismiss(); // Dismiss dialog
                        }
                    } else {
                        // Error updating details

                        String errorMessage = jsonObject.getString(
                                VolleyUtils.KEY_ERROR_MESSAGE);

                        // Toast error message
                        CustomToast.errorMessage(
                                mContext,
                                errorMessage,
                                R.drawable.ic_baseline_attach_money_24_white);

                        // Cancel Pending Request
                        ApplicationClass.getClassInstance().cancelPendingRequests(
                                NetworkTags.DebtsNetworkTags.TAG_ADD_CONTACTS_DEBT_STRING_REQUEST);
                    }
                } catch (Exception ignored) {
                }
            }, volleyError -> {

                // Log Response
                // Log.e(TAG, "Add contacts debt response error : "
                //        + volleyError.getMessage());

                ViewsUtils.dismissProgressDialog(progressDialog); // Hide Dialog

                // Check request response
                if (volleyError.getMessage() == null || volleyError instanceof NetworkError
                        || volleyError instanceof ServerError || volleyError instanceof
                        AuthFailureError || volleyError instanceof TimeoutError) {

                    CustomToast.errorMessage(mContext, DataUtils.getStringResource(mContext,
                            R.string.error_network_connection_error_message_short),
                            R.drawable.ic_sad_cloud_100px_white);

                } else {

                    // Toast Connection Error Message
                    CustomToast.errorMessage(mContext, volleyError.getMessage(),
                            R.drawable.ic_sad_cloud_100px_white);
                }

                // Clear url cache
                ApplicationClass.getClassInstance().deleteUrlVolleyCache(
                        NetworkUrls.DebtsURLS.URL_ADD_CONTACTS_DEBT);
            }) {
                @Override
                protected void deliverResponse(String response) {
                    super.deliverResponse(response);
                }

//                @Override
//                public Map<String, String> getHeaders() {
//                    HashMap<String, String> headers = new HashMap<>();
//                    headers.put("Content-Type", "application/json");
//                    // headers.put(VolleyUtils.KEY_API_KEY, VolleyUtils.getApiKey(mContext));
//                    return headers;
//                }

                @Override
                protected Map<String, String> getParams() {
                    @SuppressWarnings({"unchecked", "rawtypes"}) Map<String, String> params =
                            new HashMap();

                    // Pud debt details to Map params
                    params.put(DebtUtils.FIELD_DEBT_AMOUNT, debtAmount);
                    params.put(DebtUtils.FIELD_DEBT_DATE_ISSUED, debtDateIssued);
                    params.put(DebtUtils.FIELD_DEBT_DATE_DUE, debtDateDue);

                    // Check for debt description
                    if (!DataUtils.isEmptyString(debtDescription)) {

                        // Put debt description to Map params
                        params.put(DebtUtils.FIELD_DEBT_DESCRIPTION, debtDescription);
                    }

                    // Put userId and contactId to Map params
                    params.put(UserAccountUtils.FIELD_USER_ID, userId);
                    params.put(ContactUtils.FIELD_CONTACT_ID, contactId);

                    // Log.e(TAG, params.toString());

                    return params; // Return params
                }

                @Override
                protected VolleyError parseNetworkError(VolleyError volleyError) {
                    return super.parseNetworkError(volleyError);
                }

                @Override
                public void deliverError(VolleyError error) {
                    super.deliverError(error);
                }
            };

            // Set Request Priority
            ApplicationClass.getClassInstance().setPriority(Request.Priority.HIGH);

            // Set retry policy
            stringRequest.setRetryPolicy(new DefaultRetryPolicy(

                    DataUtils.getIntegerResource(mContext,
                            R.integer.int_volley_account_request_initial_timeout_ms),
                    DataUtils.getIntegerResource(mContext,
                            R.integer.int_volley_account_request_max_timeout_retry),
                    1.0f));

            // Set request caching to false
            stringRequest.setShouldCache(false);

            // Adding request to request queue
            ApplicationClass.getClassInstance().addToRequestQueue(stringRequest,
                    NetworkTags.DebtsNetworkTags.TAG_ADD_CONTACTS_DEBT_STRING_REQUEST);

        } else {

            // Toast network connection message
            CustomToast.errorMessage(
                    mContext,
                    DataUtils.getStringResource(mContext,
                            R.string.error_network_connection_error_message_long),
                    R.drawable.ic_sad_cloud_100px_white);
        }
    }

    @Override
    public void passDebtDateIssued(String debtDateIssuedFull, String debtDateIssuedShort) {

        this.editDebtDateIssuedFull.setText(debtDateIssuedFull); // Set full date
        this.shortDateDebtIssued = debtDateIssuedShort; // Set short date
    }

    @Override
    public void passDebtDateDue(String debtDateDueFull, String debtDateDueShort) {

        this.editDebtDateDueFull.setText(debtDateDueFull); // Set full date
        this.shortDateDebtDue = debtDateDueShort; // Set short date
    }
}
