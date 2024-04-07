package com.t4a.examples.actions;

import com.t4a.api.ActionRisk;
import com.t4a.api.JavaMethodAction;
import com.t4a.annotations.Predict;

@Predict(actionName = "restartTheECOMServer",description = "will be used to restart the server" , riskLevel = ActionRisk.HIGH, groupName = "customer support", groupDescription = "actions related to customer support")
public class ServerRestartAction implements JavaMethodAction {
    public String restartTheECOMServer(String reasonForRestart, String requestedBy) {
        return " Server has been restarted by "+requestedBy+" due to following reason "+reasonForRestart;
    }
}
