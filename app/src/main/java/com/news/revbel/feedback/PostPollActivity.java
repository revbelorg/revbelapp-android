package com.news.revbel.feedback;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.news.revbel.R;
import com.news.revbel.RevApplication;
import com.news.revbel.coordinator.NetworkCoordinator;

import java.util.List;

import javax.inject.Inject;
import javax.mail.internet.InternetAddress;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;


public class PostPollActivity extends AppCompatActivity {
    @Inject NetworkCoordinator coordinator;

    @BindView(R.id.description_2) CheckedTextView expandingText;
    @BindView(R.id.description_3) TextView fullDescription;
    @BindView(R.id.edit_email) EditText emailEdit;
    @BindView(R.id.edit_name) EditText nameEdit;
    @BindView(R.id.edit_age) EditText ageEdit;
    @BindView(R.id.edit_city) EditText cityEdit;
    @BindView(R.id.edit_work) EditText workEdit;
    @BindView(R.id.edit_anarchism) EditText anarchismEdit;
    @BindView(R.id.edit_motivation) EditText motivationEdit;
    @BindView(R.id.edit_police) EditText policeEdit;
    @BindView(R.id.edit_theory) EditText theoryEdit;
    @BindView(R.id.edit_opinion) EditText opinionEdit;
    @BindView(R.id.edit_needs) EditText needsEdit;
    @BindView(R.id.edit_participate) EditText participateEdit;
    @BindView(R.id.edit_rev_opinion) EditText revActionOpinionEdit;
    @BindView(R.id.checkbox_revbelorg) CheckBox revBelCheckbox;

    @BindString(R.string.sendpost_sended) String sentMessage;
    @BindString(R.string.sendpost_error) String errorMessage;

    public PostPollActivity() {}

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        RevApplication.getComponent().inject(this);
        setContentView(R.layout.activity_send_poll);
        ButterKnife.bind(this);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private String checkForValidation() {
        String email = emailEdit.getText().toString();
        if (email.isEmpty()) {
            Toast.makeText(this, getString(R.string.sendpoll_filledit), Toast.LENGTH_LONG).show();
            return null;
        }
        try {
            new InternetAddress(email).validate();
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.sendpoll_fillcorrectemail), Toast.LENGTH_LONG).show();
            emailEdit.requestFocus();
            return null;
        }

        String name = nameEdit.getText().toString();
        if (name.length() < 3) {
            nameEdit.requestFocus();
            Toast.makeText(this, getString(R.string.sendpoll_filleditsmall), Toast.LENGTH_LONG).show();
            return null;
        }

        int age;
        if (ageEdit.getText().toString().isEmpty()) {
            Toast.makeText(this, getString(R.string.sendpoll_filledit), Toast.LENGTH_LONG).show();
            return null;
        }
        try {
            age = Integer.parseInt(ageEdit.getText().toString());
        } catch (Exception e) {
            ageEdit.requestFocus();
            Toast.makeText(this, getString(R.string.sendpoll_fillcorrectage), Toast.LENGTH_LONG).show();
            return null;
        }

        String city = cityEdit.getText().toString();
        if (city.length() < 3) {
            cityEdit.requestFocus();
            Toast.makeText(this, getString(R.string.sendpoll_filleditsmall), Toast.LENGTH_LONG).show();
            return null;
        }

        String work = workEdit.getText().toString();
        if (work.length() < 3) {
            workEdit.requestFocus();
            Toast.makeText(this, getString(R.string.sendpoll_filleditsmall), Toast.LENGTH_LONG).show();
            return null;
        }

        String anarchism = anarchismEdit.getText().toString();
        if (anarchism.length() < 10) {
            anarchismEdit.requestFocus();
            Toast.makeText(this, getString(R.string.sendpoll_filleditbig), Toast.LENGTH_LONG).show();
            return null;
        }

        String motivation = motivationEdit.getText().toString();
        if (motivation.length() < 10) {
            motivationEdit.requestFocus();
            Toast.makeText(this, getString(R.string.sendpoll_filleditbig), Toast.LENGTH_LONG).show();
            return null;
        }

        String police = policeEdit.getText().toString();

        String theory = theoryEdit.getText().toString();

        String opinion = opinionEdit.getText().toString();
        if (opinion.length() < 10) {
            opinionEdit.requestFocus();
            Toast.makeText(this, getString(R.string.sendpoll_filleditbig), Toast.LENGTH_LONG).show();
            return null;
        }

        String needs = needsEdit.getText().toString();
        if (needs.length() < 10) {
            needsEdit.requestFocus();
            Toast.makeText(this, getString(R.string.sendpoll_filleditbig), Toast.LENGTH_LONG).show();
            return null;
        }

        String participate = participateEdit.getText().toString();
        if (participate.length() < 10) {
            participateEdit.requestFocus();
            Toast.makeText(this, getString(R.string.sendpoll_filleditbig), Toast.LENGTH_LONG).show();
            return null;
        }

        String revActionOpinion = revActionOpinionEdit.getText().toString();

        boolean checked = revBelCheckbox.isChecked();
        if (!checked) {
            revBelCheckbox.requestFocus();
            Toast.makeText(this, getString(R.string.sendpoll_fillcheck), Toast.LENGTH_LONG).show();
            return null;
        }

        String text = "Email: " + email + "\nИмя: " + name + "\nВозраст: " + age + "\nГород: " + city;
        text += "\n\nМесто работы: " + work + "\n\nОбстоятельства знакомства с анархизмом и с РД:\n" + anarchism;
        text += "\n\nМотивация участия в анархическом движении:\n" + motivation;
        if (!police.isEmpty()) text += "\n\nБыли ли приводы в милицию:\n" + police;
        if (!theory.isEmpty()) text += "\n\nЗнакомство с теорией:\n" + theory;
        text += "\n\nХарактеристика анархизма:" + opinion;
        text += "\n\nЧто нужно сейчас анархическому движению:\n" + needs + "\n\nУчастие в анархическом движении:" + participate;
        if (!revActionOpinion.isEmpty()) text += "\n\nМысли об анархическом движении и Революционном Действии:\n" + revActionOpinion;

        return text;
    }

    @OnClick(R.id.send_button)
    void onSendButtonClick() {
        String body = checkForValidation();
        if (body != null) {
            ProgressDialog progressDialog = ProgressDialog.show(this, getString(R.string.sendpost_progress_title), getString(R.string.sendpost_progress_message), true);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            coordinator.sendPoll(emailEdit.getText().toString(), nameEdit.getText().toString(), body, new NetworkCoordinator.NetworkCoordinatorErrorCallback() {
            @Override
                public void onFailure(Exception e) {
                    progressDialog.dismiss();
                    Toast.makeText(PostPollActivity.this, errorMessage + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                }

                @Override
                public void onNetworkResponse(List items) {
                    progressDialog.dismiss();
                    Toast.makeText(PostPollActivity.this, sentMessage, Toast.LENGTH_LONG).show();
                    finish();
                }
            });
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @OnClick(R.id.description_2)
    void onDesctiptionClicked() {
        if (fullDescription.getVisibility() == View.GONE) {
            expandingText.setCheckMarkDrawable(R.drawable.collaps_poll);
            fullDescription.setVisibility(View.VISIBLE);
        } else {
            expandingText.setCheckMarkDrawable(R.drawable.expand_poll);
            fullDescription.setVisibility(View.GONE);
        }
    }
}
