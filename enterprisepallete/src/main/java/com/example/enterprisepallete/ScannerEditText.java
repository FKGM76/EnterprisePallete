package com.example.enterprisepallete;

import android.content.Context;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Spinner;
import com.symbol.emdk.EMDKBase;
import com.symbol.emdk.EMDKManager;
import com.symbol.emdk.EMDKResults;
import com.symbol.emdk.barcode.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ScannerEditText extends android.support.v7.widget.AppCompatEditText implements EMDKManager.EMDKListener, Scanner.DataListener, Scanner.StatusListener, BarcodeManager.ScannerConnectionListener {

    private EMDKManager emdkManager = null;
    private BarcodeManager barcodeManager = null;
    private Scanner scanner = null;
    private Spinner spinnerTriggers = null;
    private boolean bContinuousMode = false;
    private Context mContext = null;

    private List<ScannerInfo> deviceList = null;

    private int scannerIndex = 0; // Keep the selected scanner
    private int defaultIndex = 0; // Keep the default scanner
    private int triggerIndex = 0;
    private int dataLength = 0;
    private String statusString = "";

    private String[] triggerStrings = {"HARD", "SOFT"};

    public ScannerEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context.getApplicationContext();
        emdkInitializer(mContext);
    }


    public void emdkInitializer(Context context) {
        deviceList = new ArrayList<ScannerInfo>();
        EMDKResults results = EMDKManager.getEMDKManager(mContext, this);
        if (results.statusCode != EMDKResults.STATUS_CODE.SUCCESS) {
            //textViewStatus.setText("Status: " + "EMDKManager object request failed!");
            System.out.println("Ankit == emdk status EMDKManager object request failed!");
        } else {
            System.out.println("Ankit == emdk status EMDKManager object request Passed!");
        }

        // Acquire the barcode manager resources
        if (emdkManager != null) {
            barcodeManager = (BarcodeManager) emdkManager.getInstance(EMDKManager.FEATURE_TYPE.BARCODE);
            System.out.println("Ankit == emdk status EMDKManager object request failed!");

            // Add connection listener
            if (barcodeManager != null) {
                barcodeManager.addConnectionListener(this);
            }

            // Enumerate scanner devices
            enumerateScannerDevices();

            // Set selected scanner
            // Initialize scanner
            initScanner();
            //setTrigger();
            scanner.triggerType = Scanner.TriggerType.SOFT_ALWAYS;
            startScan();
        }

    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
        System.out.println("Ankit " + focused);
        if (focused) {

            System.out.println("Ankit ===== Scanner edittext got Focus");
            startScan();
        } else {
            try {
                scanner.disable();
            } catch (ScannerException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onOpened(EMDKManager emdkManager) {

        //textViewStatus.setText("Status: " + "EMDK open success!");

        this.emdkManager = emdkManager;

        // Acquire the barcode manager resources
        barcodeManager = (BarcodeManager) emdkManager.getInstance(EMDKManager.FEATURE_TYPE.BARCODE);

        // Add connection listener
        if (barcodeManager != null) {
            barcodeManager.addConnectionListener(this);
        }

        // Enumerate scanner devices
        enumerateScannerDevices();
    }

    @Override
    public void onClosed() {

        if (emdkManager != null) {
            // Remove connection listener
            if (barcodeManager != null) {
                barcodeManager.removeConnectionListener(this);
                barcodeManager = null;
            }

            // Release all the resources
            emdkManager.release();
            emdkManager = null;
        }
        //   textViewStatus.setText("Status: " + "EMDK closed unexpectedly! Please close and restart the application.");
    }

    @Override
    public void onData(ScanDataCollection scanDataCollection) {

        if ((scanDataCollection != null) && (scanDataCollection.getResult() == ScannerResults.SUCCESS)) {
            ArrayList<ScanDataCollection.ScanData> scanData = scanDataCollection.getScanData();
            for (ScanDataCollection.ScanData data : scanData) {

                String dataString = data.getData();

                new AsyncDataUpdate().execute(dataString);
            }
        }
    }

    private void enumerateScannerDevices() {

        if (barcodeManager != null) {

            List<String> friendlyNameList = new ArrayList<String>();
            int spinnerIndex = 0;

            deviceList = barcodeManager.getSupportedDevicesInfo();

            if ((deviceList != null) && (deviceList.size() != 0)) {

                Iterator<ScannerInfo> it = deviceList.iterator();
                while (it.hasNext()) {
                    ScannerInfo scnInfo = it.next();
                    friendlyNameList.add(scnInfo.getFriendlyName());
                    if (scnInfo.isDefaultScanner()) {
                        defaultIndex = spinnerIndex;
                    }
                    ++spinnerIndex;
                }
            } else {
                //textViewStatus.setText("Status: " + "Failed to get the list of supported scanner devices! Please close and restart the application.");
            }

            ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_spinner_item, friendlyNameList);
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        }
    }

    private void setTrigger() {

        if (scanner == null) {
            initScanner();
        }

        if (scanner != null) {
            switch (1) {
                case 0: // Selected "HARD"
                    scanner.triggerType = Scanner.TriggerType.HARD;
                    break;
                case 1: // Selected "SOFT"
                    scanner.triggerType = Scanner.TriggerType.SOFT_ALWAYS;
                    break;
            }
        }
    }


    private void startScan() {

        bContinuousMode = true;

        if (scanner == null) {
            initScanner();
        }

        if (scanner != null) {
            try {

                if (!scanner.isEnabled()) {
                    scanner.enable();
                }
                scanner.read();


            } catch (ScannerException e) {

                // textViewStatus.setText("Status: " + e.getMessage());
            }
        }

    }

    private void stopScan() {

        if (scanner != null) {

            try {

                // Reset continuous flag
                bContinuousMode = false;

                // Cancel the pending read.
                scanner.cancelRead();

                new AsyncUiControlUpdate().execute(true);

            } catch (ScannerException e) {

                //textViewStatus.setText("Status: " + e.getMessage());
            }
        }
    }

    private void initScanner() {

        if (scanner == null) {

            if ((deviceList != null) && (deviceList.size() != 0)) {
                scanner = barcodeManager.getDevice(deviceList.get(1));
                System.out.println("Ankit == scanner=" + scanner.getScannerInfo().getFriendlyName());

            } else {
                //  textViewStatus.setText("Status: " + "Failed to get the specified scanner device! Please close and restart the application.");
                return;
            }

            if (scanner != null) {

                scanner.addDataListener(this);
                scanner.addStatusListener(this);

                try {
                    scanner.enable();
                } catch (ScannerException e) {

                    //textViewStatus.setText("Status: " + e.getMessage());
                }
            } else {
                // textViewStatus.setText("Status: " + "Failed to initialize the scanner device.");
            }
        }
    }

    private void deInitScanner() {

        if (scanner != null) {

            try {

                scanner.cancelRead();
                scanner.disable();

            } catch (Exception e) {

                // textViewStatus.setText("Status: " + e.getMessage());
            }

            try {
                scanner.removeDataListener(this);
                scanner.removeStatusListener(this);

            } catch (Exception e) {

                //  textViewStatus.setText("Status: " + e.getMessage());
            }

            try {
                scanner.release();
            } catch (Exception e) {

                //  textViewStatus.setText("Status: " + e.getMessage());
            }

            scanner = null;
        }
    }


    @Override
    public void onStatus(StatusData statusData) {

        StatusData.ScannerStates state = statusData.getState();
        switch (state) {
            case IDLE:
                statusString = statusData.getFriendlyName() + " is enabled and idle...";
                // new AsyncStatusUpdate().execute(statusString);
                if (bContinuousMode) {
                    try {
                        // An attempt to use the scanner continuously and rapidly (with a delay < 100 ms between scans)
                        // may cause the scanner to pause momentarily before resuming the scanning.
                        // Hence add some delay (>= 100ms) before submitting the next read.
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        scanner.triggerType = Scanner.TriggerType.SOFT_ONCE;
                        scanner.read();
                    } catch (ScannerException e) {
                        statusString = e.getMessage();
                        // new AsyncStatusUpdate().execute(statusString);
                    }
                }
                new AsyncUiControlUpdate().execute(true);
                break;
            case WAITING:
                statusString = "Scanner is waiting for trigger press...";
                new AsyncStatusUpdate().execute(statusString);
                new AsyncUiControlUpdate().execute(false);
                break;
            case SCANNING:
                statusString = "Scanning...";
                new AsyncStatusUpdate().execute(statusString);
                new AsyncUiControlUpdate().execute(false);
                break;
            case DISABLED:
                statusString = statusData.getFriendlyName() + " is disabled.";
                new AsyncStatusUpdate().execute(statusString);
                new AsyncUiControlUpdate().execute(true);
                break;
            case ERROR:
                statusString = "An error has occurred.";
                new AsyncStatusUpdate().execute(statusString);
                new AsyncUiControlUpdate().execute(true);
                break;
            default:
                break;
        }
    }

    private class AsyncDataUpdate extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {

            return params[0];
        }

        protected void onPostExecute(String result) {

            if (result != null) {
                if (dataLength++ > 100) { //Clear the cache after 100 scans
                    // textViewData.setText("");
                    dataLength = 0;
                }
                ScannerEditText.this.setText(result);
                //  textViewData.append(result+"\n");

            }
        }
    }

    private class AsyncStatusUpdate extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {

            return params[0];
        }

        @Override
        protected void onPostExecute(String result) {
            //ScannerEditText.this.setText("Status: " + result);
        }
    }

    private class AsyncUiControlUpdate extends AsyncTask<Boolean, Void, Boolean> {


        @Override
        protected void onPostExecute(Boolean bEnable) {
        }

        @Override
        protected Boolean doInBackground(Boolean... arg0) {

            return arg0[0];
        }
    }

    @Override
    public void onConnectionChange(ScannerInfo scannerInfo, BarcodeManager.ConnectionState connectionState) {

        String status;
        String scannerName = "";

        String statusExtScanner = connectionState.toString();
        String scannerNameExtScanner = scannerInfo.getFriendlyName();

        if (deviceList.size() != 0) {
            scannerName = deviceList.get(scannerIndex).getFriendlyName();
        }

        if (scannerName.equalsIgnoreCase(scannerNameExtScanner)) {

            switch (connectionState) {
                case CONNECTED:
                    deInitScanner();
                    initScanner();
                    setTrigger();
                    break;
                case DISCONNECTED:
                    deInitScanner();
                    new AsyncUiControlUpdate().execute(true);
                    break;
            }

            status = scannerNameExtScanner + ":" + statusExtScanner;
            new AsyncStatusUpdate().execute(status);
        } else {
            status = statusString + " " + scannerNameExtScanner + ":" + statusExtScanner;
            new AsyncStatusUpdate().execute(status);
        }
    }

}
