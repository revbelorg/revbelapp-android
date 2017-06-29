package com.news.revbel.donate;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.vending.billing.IInAppBillingService;
import com.news.revbel.R;
import com.news.revbel.utilities.ControlActivityInterface;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

public class DonateFragment extends Fragment {
    private Unbinder unbinder;
    private IInAppBillingService billingService;
    private ServiceConnection serviceConnection;

    private ControlActivityInterface activityInterface;

    public static DonateFragment newInstance() {
        DonateFragment fragment = new DonateFragment();

        return fragment;
    }

    public DonateFragment() {
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (getActivity() instanceof ControlActivityInterface) {
            activityInterface = (ControlActivityInterface) getActivity();
        }
        if (activityInterface != null) {
            activityInterface.updateControlButtonTapEvent(this, () -> {
            });
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (activityInterface != null) {
            activityInterface.onFragmentHide(this);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
        if (billingService != null) {
            getActivity().unbindService(serviceConnection);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_donate, container, false);
        unbinder = ButterKnife.bind(this, view);
        if (activityInterface != null) activityInterface.hideControlButton(this);

        return view;
    }

    @OnClick(R.id.donate)
    void onClick() {
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceDisconnected(ComponentName name) {
                billingService = null;
            }

            @Override
            public void onServiceConnected(ComponentName name,
                                           IBinder service) {
                billingService = IInAppBillingService.Stub.asInterface(service);
                bindService();
            }
        };
        Intent serviceIntent =
                new Intent("com.android.vending.billing.InAppBillingService.BIND");
        serviceIntent.setPackage("com.android.vending");
        boolean result = getActivity().bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        Log.d("Log", "Has result of binding billing service: " + result);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001) {
            int responseCode = data.getIntExtra("RESPONSE_CODE", 0);
            String purchaseData = data.getStringExtra("INAPP_PURCHASE_DATA");
            String dataSignature = data.getStringExtra("INAPP_DATA_SIGNATURE");

            if (resultCode == 0) {
                try {
                    JSONObject jo = new JSONObject(purchaseData);
                    String sku = jo.getString("productId");
                    String token = jo.getString("purchaseToken");
                    int response = billingService.consumePurchase(3, getActivity().getPackageName(), token);
                }
                catch (Exception e) {
                }
            }
        }

    }

    private void bindService() {
        String packageName = getActivity().getPackageName();

        Bundle querySkus = new Bundle();
        ArrayList<String> items = new ArrayList<>();
        items.add("donation");
        querySkus.putStringArrayList("ITEM_ID_LIST", items);
        Bundle skuDetails = null;
        try {
            skuDetails = billingService.getSkuDetails(3,
                    packageName, "inapp", querySkus);
            if (skuDetails != null) {
                int response = skuDetails.getInt("RESPONSE_CODE");
                if (response == 0) {
                    Bundle buyIntentBundle = billingService.getBuyIntent(3, packageName,
                            "donation", "inapp", "bGoa+V7g/yqDXvKRqq+JTFn4uQZbPiQJo4pf9RzJ");
                    PendingIntent pendingIntent = buyIntentBundle.getParcelable("BUY_INTENT");
                    startIntentSenderForResult(pendingIntent.getIntentSender(),
                            1001, new Intent(), 0, 0, 0, buyIntentBundle);
                }
            }
        } catch (Exception e) {

        }

    }
}
