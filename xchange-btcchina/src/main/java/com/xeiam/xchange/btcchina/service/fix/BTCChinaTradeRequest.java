package com.xeiam.xchange.btcchina.service.fix;

import java.math.BigDecimal;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.xml.bind.DatatypeConverter;

import quickfix.field.Account;
import quickfix.field.ClOrdID;
import quickfix.field.MassStatusReqID;
import quickfix.field.MassStatusReqType;
import quickfix.field.OrdType;
import quickfix.field.OrderID;
import quickfix.field.OrderQty;
import quickfix.field.OrigClOrdID;
import quickfix.field.Price;
import quickfix.field.Side;
import quickfix.field.Symbol;
import quickfix.field.TransactTime;
import quickfix.fix44.NewOrderSingle;
import quickfix.fix44.OrderCancelRequest;
import quickfix.fix44.OrderMassStatusRequest;
import quickfix.fix44.OrderStatusRequest;

import com.xeiam.xchange.btcchina.BTCChinaUtils;
import com.xeiam.xchange.btcchina.service.fix.field.AccReqID;
import com.xeiam.xchange.btcchina.service.fix.fix44.AccountInfoRequest;

public final class BTCChinaTradeRequest {

  private BTCChinaTradeRequest() {

  }

  public static AccountInfoRequest createAccountInfoRequest(String accessKey, String secretKey, String accReqId) {

    String methodString = "method=getAccountInfo&params=balance";
    String account = getAccountString(accessKey, secretKey, methodString);

    AccountInfoRequest message = new AccountInfoRequest();
    message.set(new Account(account));
    message.set(new AccReqID(accReqId));
    return message;
  }

  public static NewOrderSingle createNewOrderSingle(String accessKey, String secretKey, String clOrdId, char side, char ordType, BigDecimal orderQty, BigDecimal price, String symbol) {

    String methodString = String.format("method=%s&params=%s,%s,%s", side == Side.BUY ? "buyOrder3" : "sellOrder3", price == null ? "" : price.stripTrailingZeros().toPlainString(), orderQty.stripTrailingZeros().toPlainString(), symbol);
    String account = getAccountString(accessKey, secretKey, methodString);

    NewOrderSingle message = new NewOrderSingle(new ClOrdID(clOrdId), new Side(side), new TransactTime(), new OrdType(ordType));
    message.set(new Account(account));
    message.set(new OrderQty(orderQty));
    if (price != null) {
      message.set(new Price(price));
    }
    message.set(new Symbol(symbol));
    return message;
  }

  public static OrderCancelRequest createOrderCancelRequest(String accessKey, String secretKey, String clOrdId, String orderId, String symbol) {

    String methodString = String.format("method=cancelOrder3&params=%s,%s", orderId, symbol);
    String account = getAccountString(accessKey, secretKey, methodString);

    // OrigClOrdID and Side are required, but insignificant.
    OrderCancelRequest message = new OrderCancelRequest(new OrigClOrdID("DUMMY"), new ClOrdID(clOrdId), new Side(Side.SELL), new TransactTime());
    message.set(new Account(account));
    message.set(new Symbol(symbol));
    message.set(new OrderID(orderId));
    return message;
  }

  public static OrderMassStatusRequest createOrderMassStatusRequest(String accessKey, String secretKey, String massStatusReqId, int massStatusReqType, String symbol) {

    String methodString = String.format("method=getOrders&params=1,%s,1000,0,0,1", symbol);
    String account = getAccountString(accessKey, secretKey, methodString);

    OrderMassStatusRequest message = new OrderMassStatusRequest(new MassStatusReqID(massStatusReqId), new MassStatusReqType(massStatusReqType));
    message.set(new Side(Side.BUY)); // required, but insignificant
    message.set(new Account(account));
    message.set(new Symbol(symbol));
    return message;
  }

  public static OrderStatusRequest createOrderStatusRequest(String accessKey, String secretKey, String clOrdId, String orderId, String symbol) {

    String methodString = String.format("method=getOrder&params=%s,%s,1", orderId, symbol);
    String account = getAccountString(accessKey, secretKey, methodString);

    // Side is required, but insignificant.
    OrderStatusRequest message = new OrderStatusRequest(new ClOrdID(clOrdId), new Side(Side.BUY));
    message.set(new Account(account));
    message.set(new Symbol(symbol));
    message.set(new OrderID(orderId));
    return message;
  }

  private static String getAccountString(String accessKey, String secretKey, String methodString) {

    final long tonce = BTCChinaUtils.getNonce();
    final String params = String.format("tonce=%d&accesskey=%s&requestmethod=post&id=1&%s", tonce, accessKey, methodString);

    final String hash;
    try {
      hash = BTCChinaUtils.getSignature(params, secretKey);
    } catch (InvalidKeyException e) {
      throw new IllegalArgumentException(e);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalArgumentException(e);
    }

    final String userpass = accessKey + ":" + hash;
    final String basicAuth = "Basic " + DatatypeConverter.printBase64Binary(userpass.getBytes());

    return tonce + ":" + basicAuth;
  }

}
