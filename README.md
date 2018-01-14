# Salesforce-Quickbooks-Integration
Salesforce - Quickbooks Online Integration - oAuth 2.O

![Logo](https://github.com/amitastreait/Salesforce-Quickbooks-Integration/blob/master/Salesforce%20-%20QuickBooks/Images/Salesforce%20-%20QuickBooks.jpg)

Hi All, Hope you all enjoyed my previous Integration related posts. I have come up with another integration. In this post we will make connection between Salesforce and Quickbook System using Apex, VF page.

Earlier Quickbooks uses oAuth 1.0 for the authentication but now it has updated it's API and using oAuth 2.0 so in this Integration we are going to use oAuth 2.0

**Step1 -** Create a Custom Object OR Custom Metadata to store the information about Access and Refresh Token. If you are thinking about to use Custom Setting the answer is No because Access Token length is more than 255 Character and in Custom Setting we can store data upto 255 Character.

**Setup -> Create -> Objects -> New -> **Your Object will look like below image

![QuickBooks Infos Object](https://github.com/amitastreait/Salesforce-Quickbooks-Integration/blob/master/Salesforce%20-%20QuickBooks/Images/QuickBooks%20Infos%20Object.png)

**Step2 -** Create a Trail Account of QuickBooks From [Here](https://developer.intuit.com/docs/00_quickbooks_online/1_get_started/10_create_an_intuit_developer_account).

**Step3 -** Create a connected App into QuickBooks Developer account, Follow the steps given into this Link. Please enter a redirect URI. for example if you want to redirect into **"QuickbookConnection"** VF page and your org base URL is **"https://dreamhouse-a-dev-ed.my.salesforce.com" ** then your Redirect URI will be as given below

https://dreamhouse-a-dev-ed--c.ap5.visual.force.com/apex/QuickbookConnection

**Step4 -** Now, in this Step **get the Consumer Secret and Consumer Key** and to get the key follow the steps given in This [Link](https://developer.intuit.com/docs/00_quickbooks_online/1_get_started/40_get_development_keys).

![Connected App](https://github.com/amitastreait/Salesforce-Quickbooks-Integration/blob/master/Salesforce%20-%20QuickBooks/Images/Connected%20App.png)

**Step5 -** Now, Create a Apex Class. **File -> New -> Apex Class -> Name it "QuickbookConnection" -> OK**. Use below code the class

```
public class QuickbookConnection{
 // Replase with your Client Id
 public static String client_Id = 'Q0lUVQVbYhzGMzNuBe2AFc8AiQL82BPOBpIILFlqteae8aTz8H';
 // Replase with your Client Secret
 public static String consumer_Secret = 'qjhhpmfSuB0Yk5X6aQSXc5r3DgYS9jhOLVcbBsHV';
 // Replace with Your Redirect URI
 public static String redirect_URI = 'https://dreamhouse-a-dev-ed--c.ap5.visual.force.com/apex/QuickbookConnection';

 /*
 * @Name - doAuthorizationQuickBooks
 * @Param - None
 * @Description - to get the authentication code from the QuickBooks Account
 * @ReturnType - PageReference
 */

 public static PageReference doAuthorizationQuickBooks(){

 String authorization_endpoint = 'https://appcenter.intuit.com/connect/oauth2';

 String scope = 'com.intuit.quickbooks.accounting';

 String final_EndPoint = authorization_endpoint+'?client_id='+client_Id+'&amp;response_type=code&amp;scope='+
 scope+'&amp;state=123445633443&amp;redirect_uri='+redirect_URI;

 PageReference pageRef = new PageReference(final_EndPoint);
 return pageRef;
 }
 /*
 * @Name - doFetchAccessToken
 * @Param - None
 * @Description - to get the Access Token , Refresh Token and other Information after getting the authentication code
 * @ReturnType - void
 */
 public static void doFetchAccessToken(){

 String encodedString = EncodingUtil.base64Encode(Blob.valueOf(client_Id+':'+consumer_Secret));
 String endPoint = 'https://oauth.platform.intuit.com/oauth2/v1/tokens/bearer';

 String oAuthCode = ApexPages.currentPage().getParameters().get('code');
 String requestBody = 'grant_type=authorization_code&amp;code='+oAuthCode+'&amp;redirect_uri='+redirect_URI;
 String errorMessage ='';

 HttpRequest httpReq = new HttpRequest();
 HttpResponse httpRes = new HttpResponse();
 Http http = new Http();
 httpReq.setMethod('POST');
 httpReq.setEndPoint(endPoint);
 httpReq.setHeader('Authorization' , 'Basic '+encodedString);
 httpReq.setHeader('Content-Type' , 'application/x-www-form-urlencoded');
 httpReq.setBody(requestBody);

 try{
 httpRes = http.send(httpReq);

 if(httpRes.getStatusCode() == 200){
 Map&lt;String, Object&gt; response_Map = (Map&lt;String, Object&gt;)JSON.deserializeUntyped(httpRes.getBody());
 List&lt;Quickbooks_Token_Info__c&gt; connectSettingInfos = new List&lt;Quickbooks_Token_Info__c&gt;();
 connectSettingInfos = [Select Id, Name From Quickbooks_Token_Info__c Where Name ='QuickBooks Setting Info'];
 Quickbooks_Token_Info__c quickBooksSettingInfo = new Quickbooks_Token_Info__c();

 String Name = 'QuickBooks Setting Info';
 String accessToken = (String)response_Map.get('access_token');
 String refreshToken = (String)response_Map.get('refresh_token');
 Decimal expiresIn = (Decimal)response_Map.get('expires_in');
 Decimal expiresInRefToken = (Decimal)response_Map.get('x_refresh_token_expires_in');

 quickBooksSettingInfo.Name = Name;
 quickBooksSettingInfo.Access_Token__c = accessToken;
 quickBooksSettingInfo.Refresh_Token__c = refreshToken;
 quickBooksSettingInfo.Expire_In_Seconds__c = expiresIn;
 quickBooksSettingInfo.Refresh_Token_Expires_In__c = expiresInRefToken;
 if(connectSettingInfos!=null &amp;&amp; connectSettingInfos.size() &gt; 0 ) quickBooksSettingInfo.Id = connectSettingInfos[0].Id;

 upsert quickBooksSettingInfo;
 ApexPages.addmessage(new ApexPages.message(ApexPages.severity.Confirm,'Successfully Authenticated with Quickbooks System!!!'));
 }else{
 ApexPages.addmessage(new ApexPages.message(ApexPages.severity.ERROR,'Unexpected Error while communicating with Quickbooks API'+
 'Status '+httpRes.getStatus()+' and Status Code '+httpRes.getStatuscode()));
 }

 }catch(System.Exception e){
 System.debug('#### Exception Executed '+e.getStackTraceString());
 if(String.valueOf(e.getMessage()).startsWith('Unauthorized endpoint')){
 errorMessage = 'Unauthorize endpoint: An Administer must go to Setup -&gt; Administer -&gt; Security Control -&gt;'
 +' Remote Site Setting and add '+' '+ endPoint +' Endpoint';
 ApexPages.addmessage(new ApexPages.message(ApexPages.severity.ERROR,errorMessage));
 //return null;
 }else{
 errorMessage = 'Unexpected Error while communicating with Quickbooks API. '
 +'Status '+httpRes.getStatus()+' and Status Code '+httpRes.getStatuscode();
 ApexPages.addmessage(new ApexPages.message(ApexPages.severity.ERROR,errorMessage));
 //return null;
 }
 }
 }
 /*
 * @Name - doRefreshAccessToken
 * @Param - None
 * @Description - to get the Refresh Token and other Information after access token expires
 * @ReturnType - void
 */
 public static void doRefreshAccessToken(){
 String encodedString = EncodingUtil.base64Encode(Blob.valueOf(client_Id+':'+consumer_Secret));
 String endPoint = 'https://oauth.platform.intuit.com/oauth2/v1/tokens/bearer';

 QuickBookIntegrationInfo__c QBInfo = QuickBookIntegrationInfo__c.getAll().get('QuickBooks Setting Info');

 String oAuthCode = ApexPages.currentPage().getParameters().get('code');
 String requestBody = 'grant_type=refresh_token&amp;refresh_token=';
 if(QBInfo!=null &amp;&amp; QBInfo.Refresh_Token__c !=null){
 requestBody+= QBInfo.Refresh_Token__c;
 }else{
 ApexPages.addmessage(new ApexPages.message(ApexPages.severity.ERROR,'Refresh Token is NULL'));
 }
 String errorMessage ='';

 HttpRequest httpReq = new HttpRequest();
 HttpResponse httpRes = new HttpResponse();
 Http http = new Http();
 httpReq.setMethod('POST');
 httpReq.setEndPoint(endPoint);
 httpReq.setHeader('Authorization' , 'Basic '+encodedString);
 httpReq.setHeader('Content-Type' , 'application/x-www-form-urlencoded');
 httpReq.setBody(requestBody);

 try{

 }catch(System.Exception e){
 System.debug('#### Exception Executed '+e.getStackTraceString());
 if(String.valueOf(e.getMessage()).startsWith('Unauthorized endpoint')){
 errorMessage = 'Unauthorize endpoint: An Administer must go to Setup -&gt; Administer -&gt; Security Control -&gt;'
 +' Remote Site Setting and add '+' '+ endPoint +' Endpoint';
 ApexPages.addmessage(new ApexPages.message(ApexPages.severity.ERROR,errorMessage));
 //return null;
 }else{
 errorMessage = 'Unexpected Error while communicating with Quickbooks API. '
 +'Status '+httpRes.getStatus()+' and Status Code '+httpRes.getStatuscode();
 ApexPages.addmessage(new ApexPages.message(ApexPages.severity.ERROR,errorMessage));
 //return null;
 }
 }
 }
}

```



**Step5 -** Create a VF page. **File -> New -> Visualforce Page -> Name "QuickbookConnection" -> OK**. and use below code for this page

```

<apex:page controller="QuickbookConnection">
<apex:slds />
<apex:form id="theForm" >
<apex:pageblock >
<apex:pageMessages ></apex:pageMessages>
<apex:actionstatus id="statusAuthQuickBooks">
<apex:facet name="start">
<div class="waitingSearchDiv" id="el_loading" style="background-color: #fbfbfb; height:100%;opacity:0.65;width:100%;">
<div class="waitingHolder" style="top: 100px; width: 91px;">
<img class="waitingImage" src="/img/loading.gif" title="Please Wait..." />
<span class="waitingDescription">Loading...</span>
</div>
</div>
</apex:facet>
</apex:actionstatus>
<apex:pageBlockButtons location="top">
<apex:commandButton action="{!doAuthorizationQuickBooks}" value="Authorize with Quickbooks" />
<apex:commandButton action="{!doFetchAccessToken}" value="Complete Authorzation" status="statusAuthQuickBooks"
reRender="theForm" />
</apex:pageBlockButtons>
</apex:pageblock>
</apex:form>
</apex:page>

```

tada we have done with the coding and configuration part, now time to test the functionality. Click preview on the VF page. Click on **Authorize with QuickBooks** it will take you to login page of QuickBooks **login with your username and password**.

If you have more than 1 Sandbox it will list all the sandbox over there select one which you want to integrate.

Authorize the Application and you will be redirected to the same page now **click on Complete Authorization** it will create a record for the custom Object that you have created with success message

![Setp1](https://github.com/amitastreait/Salesforce-Quickbooks-Integration/blob/master/Salesforce%20-%20QuickBooks/Images/Page%201.png)
![Step2](https://github.com/amitastreait/Salesforce-Quickbooks-Integration/blob/master/Salesforce%20-%20QuickBooks/Images/Page%202.png)
![Setp3](https://github.com/amitastreait/Salesforce-Quickbooks-Integration/blob/master/Salesforce%20-%20QuickBooks/Images/Page%203.png)
![Step4](https://github.com/amitastreait/Salesforce-Quickbooks-Integration/blob/master/Salesforce%20-%20QuickBooks/Images/Page%204.png)
![FinalOutPut](https://github.com/amitastreait/Salesforce-Quickbooks-Integration/blob/master/Salesforce%20-%20QuickBooks/Images/Page%205.png)

See the working in below gif Image

![Salesforce - Quickbooks Working.gif](https://github.com/amitastreait/Salesforce-Quickbooks-Integration/blob/master/Salesforce%20-%20QuickBooks/Images/Salesforce%20-%20Quickbooks%20Working.gif)

Now, We have access token we can make the callout to get the data from QuickBooks into Salesforce.

If you have any problem then please come up into comment section.

Happy Learning :100: :joy:

**Resources -** 

> [Create Quickbooks App](https://developer.intuit.com/docs/00_quickbooks_online/1_get_started/30_create_an_app)

> [QuickBooks oAuth2.0 Steps](https://developer.intuit.com/docs/00_quickbooks_online/1_get_started/00_get_started)
    


