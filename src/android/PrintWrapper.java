package cordova.plugin.print;

import org.apache.cordova.*;
import android.content.Context;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.rt.printerlibrary.bean.SerialPortConfigBean;
import com.rt.printerlibrary.cmd.Cmd;
import com.rt.printerlibrary.cmd.EscFactory;
import com.rt.printerlibrary.connect.PrinterInterface;
import com.rt.printerlibrary.enumerate.CommonEnum;
import com.rt.printerlibrary.enumerate.SettingEnum;
import com.rt.printerlibrary.exception.SdkException;
import com.rt.printerlibrary.factory.cmd.CmdFactory;
import com.rt.printerlibrary.factory.connect.PIFactory;
import com.rt.printerlibrary.factory.connect.SerailPortFactory;
import com.rt.printerlibrary.factory.printer.PrinterFactory;
import com.rt.printerlibrary.factory.printer.ThermalPrinterFactory;
import com.rt.printerlibrary.observer.PrinterObserver;
import com.rt.printerlibrary.observer.PrinterObserverManager;
import com.rt.printerlibrary.printer.RTPrinter;
import com.rt.printerlibrary.setting.TextSetting;
import com.rt.printerlibrary.utils.PrinterPowerUtil;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import java.util.List;
import java.util.ArrayList;

import static com.rt.printerlibrary.enumerate.CommonEnum.ALIGN_MIDDLE;
import static com.rt.printerlibrary.enumerate.CommonEnum.ALIGN_LEFT;


public class PrintWrapper extends CordovaPlugin implements PrinterObserver{

    private RTPrinter rtPrinter = null;
    private PrinterFactory printerFactory;
    private Object configObj;
    private PrinterPowerUtil printerPowerUtil;//To switch AP02 printer power.
    private Boolean connected = false;
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        Context context=this.cordova.getActivity().getApplicationContext();
        printerFactory = new ThermalPrinterFactory();
        rtPrinter = printerFactory.create();
        PrinterObserverManager.getInstance().add(this);
        configObj = new SerialPortConfigBean().getDefaultConfig();
        printerPowerUtil = new PrinterPowerUtil(context);
        connect();
     } 
 

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
       
        if(action.equals("printMethod")){
                this.printReceipt(args, callbackContext);
                return true;
        }

        if(action.equals("printIntlMethod")){
                this.printIntltopupReceipt(args, callbackContext);
                return true;
        }

        
        //   if(action.equals("connectMethod")){
        //         this.connect(callbackContext);
        //         return true;
        // }
        //   if(action.equals("checkConnectMethod")){
        //         this.checkConnect(callbackContext);
        //         return true;
        // }
        return false;
    }

     /**
     * Show a toast with some message
     */
    public void showToast(String toLog){
        Context context = cordova.getActivity();
        int duration = Toast.LENGTH_SHORT;

        Toast toast = Toast.makeText(context, toLog, duration);
        toast.show();
    }
    
    @Override
    public void printerObserverCallback(final PrinterInterface printerInterface, final int state) {
        switch (state) {
            case CommonEnum.CONNECT_STATE_SUCCESS:
                connected = true;
                rtPrinter.setPrinterInterface(printerInterface);
                //BaseApplication.getInstance().setRtPrinter(rtPrinter);
                break;
            case CommonEnum.CONNECT_STATE_INTERRUPTED:
                connected = false;
               // BaseApplication.getInstance().setRtPrinter(null);
                break;
            default:
                break;
        }
    }

    private void connect() {
            connectSerialPort((SerialPortConfigBean) configObj);
            printerPowerUtil.setPrinterPower(true);
    }

    

    private void printReceipt(JSONArray std, CallbackContext callbackContext) {
   
        JSONArray myjsonarray = std;
        // JSONObject jObject = new JSONObject(std);
        // JSONArray myjsonarray = jObject.getJSONArray(0);
        new Thread(new Runnable() {
            JSONArray rceiptdata;
            JSONArray item;
            {
                try{
                    this.rceiptdata = myjsonarray.getJSONArray(0);
                }catch(Exception e){
                    e.printStackTrace();
                    callbackContext.error(e.getMessage().toString());
                }
            }

            @Override
            public void run() {
                try {
                   CmdFactory escFac = new EscFactory();
                   Cmd escCmd = escFac.create();
                   escCmd.setChartsetName("UTF-8");
                    for(int i = 0; i < this.rceiptdata.length(); i++)
                    {
                        TextSetting textSetting = new TextSetting();
                        textSetting.setAlign(ALIGN_LEFT);//对齐方式-左对齐，居中，右对齐
                        textSetting.setBold(SettingEnum.Enable);//加粗
                        textSetting.setUnderline(SettingEnum.Disable);//下划线
                        textSetting.setIsAntiWhite(SettingEnum.Disable);//反白
                        textSetting.setDoubleHeight(SettingEnum.Enable);//倍高
                        textSetting.setDoubleWidth(SettingEnum.Enable);//倍宽
                        textSetting.setItalic(SettingEnum.Disable);//斜体
                        textSetting.setIsEscSmallCharactor(SettingEnum.Disable);//小字体
                        escCmd.append(escCmd.getHeaderCmd());//初始化
                        escCmd.append(escCmd.getTextCmd(textSetting, this.rceiptdata.getJSONObject(i).getString("businessname")));
                        escCmd.append(escCmd.getLFCRCmd());//回车换行

                        textSetting.setCpclFontSize(200);
                        textSetting.setIsEscSmallCharactor(SettingEnum.Disable);
                        textSetting.setBold(SettingEnum.Disable);
                        textSetting.setDoubleHeight(SettingEnum.Disable);
                        textSetting.setDoubleWidth(SettingEnum.Disable);
                        
                        escCmd.append(escCmd.getTextCmd(textSetting, this.rceiptdata.getJSONObject(i).getString("shopname")));

                        escCmd.append(escCmd.getLFCRCmd());
                        textSetting.setUnderline(SettingEnum.Enable);
                        escCmd.append(escCmd.getTextCmd(textSetting, this.rceiptdata.getJSONObject(i).getString("address")));

                        escCmd.append(escCmd.getLFCRCmd());
                        textSetting.setUnderline(SettingEnum.Enable);
                        escCmd.append(escCmd.getTextCmd(textSetting, this.rceiptdata.getJSONObject(i).getString("address1")));
                        
                        escCmd.append(escCmd.getLFCRCmd());
                        textSetting.setUnderline(SettingEnum.Enable);
                        escCmd.append(escCmd.getTextCmd(textSetting, this.rceiptdata.getJSONObject(i).getString("postcode")));
                        
                        escCmd.append(escCmd.getLFCRCmd());
                        textSetting.setUnderline(SettingEnum.Enable);
                        escCmd.append(escCmd.getTextCmd(textSetting, this.rceiptdata.getJSONObject(i).getString("transactionnumber")));
                        
                        escCmd.append(escCmd.getLFCRCmd());
                        textSetting.setUnderline(SettingEnum.Enable);
                        escCmd.append(escCmd.getTextCmd(textSetting, this.rceiptdata.getJSONObject(i).getString("transactiondate")));
                        this.item = this.rceiptdata.getJSONObject(i).getJSONArray("item");
                        for(int j = 0; i < this.item.length(); i++)
                        {
                            escCmd.append(escCmd.getLFCRCmd());
                            textSetting.setUnderline(SettingEnum.Enable);
                            escCmd.append(escCmd.getTextCmd(textSetting, this.item.getJSONObject(i).getString("networkname")));

                            escCmd.append(escCmd.getLFCRCmd());
                            textSetting.setUnderline(SettingEnum.Enable);
                            escCmd.append(escCmd.getTextCmd(textSetting, this.item.getJSONObject(i).getString("amountreceive")));

                            escCmd.append(escCmd.getLFCRCmd());
                            textSetting.setUnderline(SettingEnum.Enable);
                            escCmd.append(escCmd.getTextCmd(textSetting, this.item.getJSONObject(i).getString("pinserial")));
                            
                            escCmd.append(escCmd.getLFCRCmd());
                            textSetting.setUnderline(SettingEnum.Enable);
                            escCmd.append(escCmd.getTextCmd(textSetting, "**************************")); 

                            escCmd.append(escCmd.getLFCRCmd());
                            textSetting.setUnderline(SettingEnum.Enable);
                            escCmd.append(escCmd.getTextCmd(textSetting, this.item.getJSONObject(i).getString("pincode")));

                            escCmd.append(escCmd.getLFCRCmd());
                            textSetting.setUnderline(SettingEnum.Enable);
                            escCmd.append(escCmd.getTextCmd(textSetting, "**************************"));

                            escCmd.append(escCmd.getLFCRCmd());
                            textSetting.setUnderline(SettingEnum.Enable);
                            escCmd.append(escCmd.getTextCmd(textSetting, this.item.getJSONObject(i).getString("expirationdate")));

                            escCmd.append(escCmd.getLFCRCmd());
                            textSetting.setUnderline(SettingEnum.Enable);
                            escCmd.append(escCmd.getTextCmd(textSetting, this.item.getJSONObject(i).getString("accessnumbers")));
                        }
                        escCmd.append(escCmd.getLFCRCmd());
                        textSetting.setUnderline(SettingEnum.Enable);
                        escCmd.append(escCmd.getTextCmd(textSetting, "________________________________"));
                        textSetting.setUnderline(SettingEnum.Enable);
                        escCmd.append(escCmd.getLFCRCmd());
                        escCmd.append(escCmd.getTextCmd(textSetting, "Pins/Voucher bought at this location cannot be exchanged or refunded."));
                        escCmd.append(escCmd.getTextCmd(textSetting, " Please contact relevant operator customer service alternatively email your"));
                        escCmd.append(escCmd.getTextCmd(textSetting, "query with this receipt to sales@ipayon.com"));
                        escCmd.append(escCmd.getLFCRCmd());
                        escCmd.append(escCmd.getLFCRCmd());

                        escCmd.append(escCmd.getLFCRCmd());
                        escCmd.append(escCmd.getLFCRCmd());

                        escCmd.append(escCmd.getLFCRCmd());
                        escCmd.append(escCmd.getLFCRCmd());
                        escCmd.append(escCmd.getLFCRCmd());
                    }
                    rtPrinter.writeMsg(escCmd.getAppendCmds());
                    callbackContext.success("Print done");
                } catch (Exception e) {
                    e.printStackTrace();
                    callbackContext.error(e.getMessage().toString());
                }
            }
        }).start();
    }

    private void printIntltopupReceipt(JSONArray std, CallbackContext callbackContext) {
		//if(connected == false){
		//	callbackContext.error("error");
		//}
        JSONArray myjsonarray = std;
        // JSONObject jObject = new JSONObject(std);
        // JSONArray myjsonarray = jObject.getJSONArray(0);
        new Thread(new Runnable() {
            JSONArray rceiptdata;
            JSONArray item;
            {
                try{
                    this.rceiptdata = myjsonarray.getJSONArray(0);
                }catch(Exception e){
                    e.printStackTrace();
                    callbackContext.error(e.getMessage().toString());
                }
            }

            @Override
            public void run() {
                try {
                   CmdFactory escFac = new EscFactory();
                   Cmd escCmd = escFac.create();
                   escCmd.setChartsetName("UTF-8");
                    for(int i = 0; i < this.rceiptdata.length(); i++)
                    {
                        TextSetting textSetting = new TextSetting();
                        textSetting.setAlign(ALIGN_LEFT);//对齐方式-左对齐，居中，右对齐
                        textSetting.setBold(SettingEnum.Enable);//加粗
                        textSetting.setUnderline(SettingEnum.Disable);//下划线
                        textSetting.setIsAntiWhite(SettingEnum.Disable);//反白
                        textSetting.setDoubleHeight(SettingEnum.Enable);//倍高
                        textSetting.setDoubleWidth(SettingEnum.Enable);//倍宽
                        textSetting.setItalic(SettingEnum.Disable);//斜体
                        textSetting.setIsEscSmallCharactor(SettingEnum.Disable);//小字体
                        escCmd.append(escCmd.getHeaderCmd());//初始化
                        escCmd.append(escCmd.getTextCmd(textSetting, this.rceiptdata.getJSONObject(i).getString("businessname")));
                        escCmd.append(escCmd.getLFCRCmd());//回车换行

                        textSetting.setCpclFontSize(200);
                        textSetting.setIsEscSmallCharactor(SettingEnum.Disable);
                        textSetting.setBold(SettingEnum.Disable);
                        textSetting.setDoubleHeight(SettingEnum.Disable);
                        textSetting.setDoubleWidth(SettingEnum.Disable);
                        
                        escCmd.append(escCmd.getTextCmd(textSetting, this.rceiptdata.getJSONObject(i).getString("shopname")));

                        escCmd.append(escCmd.getLFCRCmd());
                        textSetting.setUnderline(SettingEnum.Enable);
                        escCmd.append(escCmd.getTextCmd(textSetting, this.rceiptdata.getJSONObject(i).getString("address")));

                        escCmd.append(escCmd.getLFCRCmd());
                        textSetting.setUnderline(SettingEnum.Enable);
                        escCmd.append(escCmd.getTextCmd(textSetting, this.rceiptdata.getJSONObject(i).getString("address1")));
                        
                        escCmd.append(escCmd.getLFCRCmd());
                        textSetting.setUnderline(SettingEnum.Enable);
                        escCmd.append(escCmd.getTextCmd(textSetting, this.rceiptdata.getJSONObject(i).getString("postcode")));
                        
                        escCmd.append(escCmd.getLFCRCmd());
                        textSetting.setUnderline(SettingEnum.Enable);
                        escCmd.append(escCmd.getTextCmd(textSetting, this.rceiptdata.getJSONObject(i).getString("transactionnumber")));
                        
                        escCmd.append(escCmd.getLFCRCmd());
                        textSetting.setUnderline(SettingEnum.Enable);
                        escCmd.append(escCmd.getTextCmd(textSetting, this.rceiptdata.getJSONObject(i).getString("transactiondate")));
						
						escCmd.append(escCmd.getLFCRCmd());
                        textSetting.setUnderline(SettingEnum.Enable);
                        escCmd.append(escCmd.getTextCmd(textSetting, "Int'l Mobile Topup"));
                        this.item = this.rceiptdata.getJSONObject(i).getJSONArray("item");
                        for(int j = 0; i < this.item.length(); i++)
                        {
                            escCmd.append(escCmd.getLFCRCmd());
                            textSetting.setUnderline(SettingEnum.Enable);
                            escCmd.append(escCmd.getTextCmd(textSetting, this.item.getJSONObject(i).getString("networkname")));

                            escCmd.append(escCmd.getLFCRCmd());
                            textSetting.setUnderline(SettingEnum.Enable);
                            escCmd.append(escCmd.getTextCmd(textSetting, this.item.getJSONObject(i).getString("amountpurchase")));
							
							escCmd.append(escCmd.getLFCRCmd());
                            textSetting.setUnderline(SettingEnum.Enable);
                            escCmd.append(escCmd.getTextCmd(textSetting, this.item.getJSONObject(i).getString("amountcharge")));
							
							escCmd.append(escCmd.getLFCRCmd());
                            textSetting.setUnderline(SettingEnum.Enable);
                            escCmd.append(escCmd.getTextCmd(textSetting, this.item.getJSONObject(i).getString("amountreceive")));
							
                            escCmd.append(escCmd.getLFCRCmd());
                            textSetting.setUnderline(SettingEnum.Enable);
                            escCmd.append(escCmd.getTextCmd(textSetting, this.item.getJSONObject(i).getString("taxdeducted")));
							
							escCmd.append(escCmd.getLFCRCmd());
                            textSetting.setUnderline(SettingEnum.Enable);
                            escCmd.append(escCmd.getTextCmd(textSetting, this.item.getJSONObject(i).getString("moblilenumber")));
							
							escCmd.append(escCmd.getLFCRCmd());
                            textSetting.setUnderline(SettingEnum.Enable);
                            escCmd.append(escCmd.getTextCmd(textSetting, this.item.getJSONObject(i).getString("topupamount")));
                        }
                        escCmd.append(escCmd.getLFCRCmd());
                        textSetting.setUnderline(SettingEnum.Enable);
                        escCmd.append(escCmd.getTextCmd(textSetting, "________________________________"));
                        textSetting.setUnderline(SettingEnum.Enable);
                        escCmd.append(escCmd.getLFCRCmd());
                        escCmd.append(escCmd.getTextCmd(textSetting, "Supplier of the International Mobile Topup/credit & Bill Payment are based outside the EU and mobile"));
                        escCmd.append(escCmd.getTextCmd(textSetting, "credit transferred to your  recipient are meant to be used and enjoyed outside EU only. iPayOn and nominated company are"));
                        escCmd.append(escCmd.getTextCmd(textSetting, "only acting as cash collection and marketing agency for such suppliers/operators and not the seller of such services to"));
                        escCmd.append(escCmd.getTextCmd(textSetting, "you or retailers. These services are strictly for recipients based outside EU"));
                        escCmd.append(escCmd.getLFCRCmd());
                        escCmd.append(escCmd.getLFCRCmd());

                        escCmd.append(escCmd.getLFCRCmd());
                        escCmd.append(escCmd.getLFCRCmd());
                        escCmd.append(escCmd.getLFCRCmd());
                    }
                    rtPrinter.writeMsg(escCmd.getAppendCmds());
                    callbackContext.success("Print done");
                } catch (Exception e) {
                    e.printStackTrace();
                    callbackContext.error(e.getMessage().toString());
                }
            }
        }).start();
    }

    // private void doDisConnect() {
    //     if (rtPrinter != null && rtPrinter.getPrinterInterface() != null) {
    //         rtPrinter.disConnect();
    //     }
    //     printerPowerUtil.setPrinterPower(false);//turn printer power off.
    //     setPrintEnable(false);
    // }

    private void connectSerialPort(SerialPortConfigBean serialPortConfigBean) {
        PIFactory piFactory = new SerailPortFactory();
        PrinterInterface printerInterface = piFactory.create();
        printerInterface.setConfigObject(serialPortConfigBean);
        rtPrinter.setPrinterInterface(printerInterface);
        try {
            rtPrinter.connect(serialPortConfigBean);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {

        }
    }
}
