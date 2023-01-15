package com.coyee.cache.console.controller;

import java.util.ArrayList;


public class SimilarityMain {

    public static double similarity(ArrayList va, ArrayList vb) {
        if (va.size() > vb.size()) {
            int temp = va.size() - vb.size();
            for (int i = 0; i < temp; i++) {
                vb.add(0);
            }
        } else if (va.size() < vb.size()) {
            int temp = vb.size() - va.size();
            for (int i = 0; i < temp; i++) {
                va.add(0);
            }
        }

        int size = va.size();
        double simVal = 0;


        double num = 0;
        double den = 1;
        double powa_sum = 0;
        double powb_sum = 0;
        for (int i = 0; i < size; i++) {
            double a = Double.parseDouble(va.get(i).toString());
            double b = Double.parseDouble(vb.get(i).toString());

            num = num + a * b;
            powa_sum = powa_sum + (double) Math.pow(a, 2);
            powb_sum = powb_sum + (double) Math.pow(b, 2);
        }
        double sqrta = (double) Math.sqrt(powa_sum);
        double sqrtb = (double) Math.sqrt(powb_sum);
        den = sqrta * sqrtb;

        simVal = num / den;

        return simVal;
    }

    public static void main(String[] args) {
        String item[] = {"吃苹果", "逛商店", "看电视剧", "打羽毛球", "吃桔子"};
        float a[] = {(float) 4.5, 5, 5, 5, 0};
        float b[] = {(float) 3.5, 5, 5, 5, 0};
        ArrayList vitem = new ArrayList();
        ArrayList<Float> va = new ArrayList();
        ArrayList<Float> vb = new ArrayList();
        for (int i = 0; i < a.length; i++) {
            vitem.add(item[i]);
            va.add(new Float(a[i]));
            vb.add(new Float(b[i]));
        }
        System.out.print("兴趣");
        System.out.println(vitem);
        System.out.print("小红");
        System.out.println(va);
        System.out.print("xxx");
        System.out.println(vb);

        SimilarityMain sim = new SimilarityMain();

        double simVal = sim.similarity(va, vb);

        System.out.println("The sim value is:" + simVal);
    }

}