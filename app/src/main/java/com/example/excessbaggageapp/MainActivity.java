package com.example.excessbaggageapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "ExcessBaggage";
    private static final double ECONOMY_LIMIT_KG = 20.0;
    private static final double BUSINESS_LIMIT_KG = 40.0;
    private static final double SERVICE_TAX = 0.08;

    private TextInputLayout layoutWeight, layoutRate;
    private TextInputEditText etWeight, etRate;
    private EditText activeEditText;
    private View layoutKeypadContainer;
    private RadioGroup rgClass;
    private View cardResult;
    private TextView tvExcessWeight, tvExcessCharge, tvTotal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        layoutWeight = findViewById(R.id.layoutWeight);
        layoutRate = findViewById(R.id.layoutRate);
        etWeight = findViewById(R.id.etWeight);
        etRate = findViewById(R.id.etRate);
        layoutKeypadContainer = findViewById(R.id.layoutKeypadContainer);
        rgClass = findViewById(R.id.rgClass);
        cardResult = findViewById(R.id.cardResult);
        tvExcessWeight = findViewById(R.id.tvExcessWeight);
        tvExcessCharge = findViewById(R.id.tvExcessCharge);
        tvTotal = findViewById(R.id.tvTotal);

        // Prevent soft keyboard from popping up so on-screen keypad is used
        etWeight.setShowSoftInputOnFocus(false);
        etRate.setShowSoftInputOnFocus(false);

        activeEditText = null; // No active input until user touches a field

        View.OnFocusChangeListener focusChangeListener = (v, hasFocus) -> {
            if (hasFocus) {
                activeEditText = (EditText) v;
                showKeypad();
            }
        };

        View.OnClickListener clickListener = v -> {
            activeEditText = (EditText) v;
            showKeypad();
        };

        etWeight.setOnFocusChangeListener(focusChangeListener);
        etRate.setOnFocusChangeListener(focusChangeListener);
        etWeight.setOnClickListener(clickListener);
        etRate.setOnClickListener(clickListener);

        setupKeypad();

        Button btnCalculate = findViewById(R.id.btnCalculate);
        Button btnReset = findViewById(R.id.btnReset);

        btnCalculate.setOnClickListener(v -> calculate());
        btnReset.setOnClickListener(v -> reset());
    }

    private void setupKeypad() {
        int[] numberButtonIds = {
                R.id.btnKey0, R.id.btnKey1, R.id.btnKey2, R.id.btnKey3,
                R.id.btnKey4, R.id.btnKey5, R.id.btnKey6, R.id.btnKey7,
                R.id.btnKey8, R.id.btnKey9, R.id.btnKeyDot
        };

        View.OnClickListener keyClickListener = v -> {
            Button btn = (Button) v;
            appendKeypadValue(btn.getText().toString());
        };

        for (int id : numberButtonIds) {
            findViewById(id).setOnClickListener(keyClickListener);
        }

        findViewById(R.id.btnKeyDelete).setOnClickListener(v -> deleteKeypadValue());
        findViewById(R.id.btnCloseKeypad).setOnClickListener(v -> hideKeypad());
    }

    private void showKeypad() {
        if (layoutKeypadContainer != null && layoutKeypadContainer.getVisibility() != View.VISIBLE) {
            layoutKeypadContainer.setVisibility(View.VISIBLE);
        }
    }

    private void hideKeypad() {
        if (layoutKeypadContainer != null && layoutKeypadContainer.getVisibility() != View.GONE) {
            layoutKeypadContainer.setVisibility(View.GONE);
        }
    }

    private void appendKeypadValue(String value) {
        EditText target = activeEditText != null ? activeEditText : etWeight;
        target.requestFocus();

        // Prevent multiple decimal points in a single number field
        if (".".equals(value) && target.getText() != null && target.getText().toString().contains(".")) {
            return;
        }

        int start = Math.max(target.getSelectionStart(), 0);
        int end = Math.max(target.getSelectionEnd(), 0);
        if (target.getText() != null) {
            target.getText().replace(Math.min(start, end), Math.max(start, end), value, 0, value.length());
        }
    }

    private void deleteKeypadValue() {
        EditText target = activeEditText != null ? activeEditText : etWeight;
        target.requestFocus();
        target.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
    }

    private void calculate() {
        layoutWeight.setError(null);
        layoutRate.setError(null);

        String weightText = String.valueOf(etWeight.getText()).trim();
        String rateText = String.valueOf(etRate.getText()).trim();
        boolean valid = true;

        double weight = 0, rate = 0;

        // ---- Validation (error messages) ----
        if (weightText.isEmpty()) {
            layoutWeight.setError(getString(R.string.err_weight_empty));
            valid = false;
        } else {
            try {
                weight = Double.parseDouble(weightText);
                if (weight <= 0) {
                    layoutWeight.setError(getString(R.string.err_weight_positive));
                    valid = false;
                }
            } catch (NumberFormatException e) {
                layoutWeight.setError(getString(R.string.err_invalid_number));
                valid = false;
            }
        }

        if (rateText.isEmpty()) {
            layoutRate.setError(getString(R.string.err_rate_empty));
            valid = false;
        } else {
            try {
                rate = Double.parseDouble(rateText);
                if (rate < 0) {
                    layoutRate.setError(getString(R.string.err_rate_negative));
                    valid = false;
                }
            } catch (NumberFormatException e) {
                layoutRate.setError(getString(R.string.err_invalid_number));
                valid = false;
            }
        }

        if (!valid) {
            cardResult.setVisibility(View.GONE);
            Toast.makeText(this, R.string.toast_fix_errors, Toast.LENGTH_SHORT).show();
            return;
        }

        // ---- Calculation ----
        boolean isBusiness = rgClass.getCheckedRadioButtonId() == R.id.rbBusiness;
        double limit = isBusiness ? BUSINESS_LIMIT_KG : ECONOMY_LIMIT_KG;

        double excessWeight = Math.max(0, weight - limit);   // never negative
        double excessCharge = excessWeight * rate;
        double total = excessCharge + (SERVICE_TAX * excessCharge);

        Log.d(TAG, "weight=" + weight + " limit=" + limit + " rate=" + rate
                + " excess=" + excessWeight + " charge=" + excessCharge + " total=" + total);

        // ---- Output (formatted) ----
        tvExcessWeight.setText(String.format(Locale.US, "%.2f kg", excessWeight));
        tvExcessCharge.setText(String.format(Locale.US, "RM %.2f", excessCharge));
        tvTotal.setText(String.format(Locale.US, "RM %.2f", total));
        cardResult.setVisibility(View.VISIBLE);

        if (excessWeight == 0) {
            Toast.makeText(this, getString(R.string.toast_within_limit, (int) limit),
                    Toast.LENGTH_LONG).show();
        }

        hideKeypad();
    }

    private void reset() {
        etWeight.setText("");
        etRate.setText("");
        layoutWeight.setError(null);
        layoutRate.setError(null);
        ((RadioButton) findViewById(R.id.rbEconomy)).setChecked(true);
        cardResult.setVisibility(View.GONE);
        activeEditText = null;
        hideKeypad();
    }

    // ---- Toolbar menu: Share + About ----
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_share) {
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("text/plain");
            send.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
            send.putExtra(Intent.EXTRA_TEXT,
                    getString(R.string.share_text, getString(R.string.app_url)));
            startActivity(Intent.createChooser(send, getString(R.string.share_via)));
            return true;
        } else if (id == R.id.action_about) {
            startActivity(new Intent(this, AboutActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}