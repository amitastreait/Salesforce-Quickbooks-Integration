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
        
        String final_EndPoint = authorization_endpoint+'?client_id='+client_Id+'&response_type=code&scope='+
                                    scope+'&state=123445633443&redirect_uri='+redirect_URI;
        
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
        String requestBody = 'grant_type=authorization_code&code='+oAuthCode+'&redirect_uri='+redirect_URI;
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
                Map<String, Object> response_Map = (Map<String, Object>)JSON.deserializeUntyped(httpRes.getBody());
                List<Quickbooks_Token_Info__c> connectSettingInfos = new List<Quickbooks_Token_Info__c>();
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
                if(connectSettingInfos!=null && connectSettingInfos.size() > 0 ) quickBooksSettingInfo.Id = connectSettingInfos[0].Id;
                
                upsert quickBooksSettingInfo;
                ApexPages.addmessage(new ApexPages.message(ApexPages.severity.Confirm,'Successfully Authenticated with Quickbooks System!!!'));
            }else{
                ApexPages.addmessage(new ApexPages.message(ApexPages.severity.ERROR,'Unexpected Error while communicating with Quickbooks API'+
                                         'Status '+httpRes.getStatus()+' and Status Code '+httpRes.getStatuscode()));
            }
           
        }catch(System.Exception e){
            System.debug('#### Exception Executed '+e.getStackTraceString());
             if(String.valueOf(e.getMessage()).startsWith('Unauthorized endpoint')){
                    errorMessage = 'Unauthorize endpoint: An Administer must go to Setup -> Administer -> Security Control ->'
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
        String requestBody = 'grant_type=refresh_token&refresh_token=';
        if(QBInfo!=null && QBInfo.Refresh_Token__c !=null){
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
                    errorMessage = 'Unauthorize endpoint: An Administer must go to Setup -> Administer -> Security Control ->'
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