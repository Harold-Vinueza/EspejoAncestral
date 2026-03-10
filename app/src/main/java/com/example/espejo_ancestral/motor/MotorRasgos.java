package com.example.espejo_ancestral.motor;

import android.graphics.PointF;
import android.graphics.Rect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceContour;
import com.google.mlkit.vision.face.FaceLandmark;




public class MotorRasgos {

    // =========================
    // MODELOS DE RESPUESTA
    // =========================
    public static class Rasgo {
        public String nombre;

        public Rasgo(String nombre) {
            this.nombre = nombre;
        }
    }

    public static class Resultado {
        public String perfil;
        public float confianza;
        public List<Rasgo> rasgosUI;
        public Map<String, Float> metricas;

        public Resultado(String perfil, float confianza,
                         List<Rasgo> rasgosUI,
                         Map<String, Float> metricas) {
            this.perfil = perfil;
            this.confianza = confianza;
            this.rasgosUI = rasgosUI;
            this.metricas = metricas;
        }
    }

    // =========================
    // MÉTODO PRINCIPAL
    // =========================
    public static Resultado evaluar(Face face) {

        Rect box = face.getBoundingBox();
        float faceWidth = Math.max(1f, box.width());
        float faceHeight = Math.max(1f, box.height());

        Map<String, Float> m = new HashMap<>();
        List<Rasgo> rasgos = new ArrayList<>();

        // -------------------------
        // 1) CLASIFICACIONES ML KIT
        // -------------------------
        Float sonrisa = safeProb(face.getSmilingProbability());
        Float ojoIzqAbierto = safeProb(face.getLeftEyeOpenProbability());
        Float ojoDerAbierto = safeProb(face.getRightEyeOpenProbability());

        m.put("smile", sonrisa);
        m.put("leftEyeOpen", ojoIzqAbierto);
        m.put("rightEyeOpen", ojoDerAbierto);

        // -------------------------
        // 2) ÁNGULOS / ORIENTACIÓN
        // -------------------------
        float rotX = face.getHeadEulerAngleX(); // arriba/abajo
        float rotY = face.getHeadEulerAngleY(); // izquierda/derecha
        float rotZ = face.getHeadEulerAngleZ(); // inclinación
        m.put("rotX", rotX);
        m.put("rotY", rotY);
        m.put("rotZ", rotZ);

        // -------------------------
        // 3) TRACKING
        // -------------------------
        int tracking = (face.getTrackingId() != null) ? face.getTrackingId() : -1;
        m.put("trackingId", (float) tracking);

        // -------------------------
        // 4) LANDMARKS
        // -------------------------
        PointF leftEyeLm = getLandmark(face, FaceLandmark.LEFT_EYE);
        PointF rightEyeLm = getLandmark(face, FaceLandmark.RIGHT_EYE);
        PointF noseBaseLm = getLandmark(face, FaceLandmark.NOSE_BASE);
        PointF mouthLeftLm = getLandmark(face, FaceLandmark.MOUTH_LEFT);
        PointF mouthRightLm = getLandmark(face, FaceLandmark.MOUTH_RIGHT);
        PointF mouthBottomLm = getLandmark(face, FaceLandmark.MOUTH_BOTTOM);
        PointF leftCheekLm = getLandmark(face, FaceLandmark.LEFT_CHEEK);
        PointF rightCheekLm = getLandmark(face, FaceLandmark.RIGHT_CHEEK);
        PointF leftEarLm = getLandmark(face, FaceLandmark.LEFT_EAR);
        PointF rightEarLm = getLandmark(face, FaceLandmark.RIGHT_EAR);

        // -------------------------
        // 5) CONTORNOS ML KIT
        // -------------------------
        List<PointF> faceContour = getContour(face, FaceContour.FACE);
        List<PointF> leftEyeContour = getContour(face, FaceContour.LEFT_EYE);
        List<PointF> rightEyeContour = getContour(face, FaceContour.RIGHT_EYE);
        List<PointF> upperLipTop = getContour(face, FaceContour.UPPER_LIP_TOP);
        List<PointF> upperLipBottom = getContour(face, FaceContour.UPPER_LIP_BOTTOM);
        List<PointF> lowerLipTop = getContour(face, FaceContour.LOWER_LIP_TOP);
        List<PointF> lowerLipBottom = getContour(face, FaceContour.LOWER_LIP_BOTTOM);
        List<PointF> noseBridge = getContour(face, FaceContour.NOSE_BRIDGE);
        List<PointF> noseBottom = getContour(face, FaceContour.NOSE_BOTTOM);
        List<PointF> leftEyebrowTop = getContour(face, FaceContour.LEFT_EYEBROW_TOP);
        List<PointF> rightEyebrowTop = getContour(face, FaceContour.RIGHT_EYEBROW_TOP);
        List<PointF> leftEyebrowBottom = getContour(face, FaceContour.LEFT_EYEBROW_BOTTOM);
        List<PointF> rightEyebrowBottom = getContour(face, FaceContour.RIGHT_EYEBROW_BOTTOM);
        List<PointF> leftCheekContour = getContour(face, FaceContour.LEFT_CHEEK);
        List<PointF> rightCheekContour = getContour(face, FaceContour.RIGHT_CHEEK);

        // -------------------------
        // 6) MÉTRICAS BASE
        // -------------------------

        // distancia entre ojos
        float eyeDistance = -1f;
        if (leftEyeLm != null && rightEyeLm != null) {
            eyeDistance = dist(leftEyeLm, rightEyeLm) / faceWidth;
            m.put("eyeDistanceRatio", eyeDistance);
        }

        // ancho boca
        float mouthWidth = -1f;
        if (mouthLeftLm != null && mouthRightLm != null) {
            mouthWidth = dist(mouthLeftLm, mouthRightLm) / faceWidth;
            m.put("mouthWidthRatio", mouthWidth);
        }

        // posición vertical nariz
        if (noseBaseLm != null) {
            float noseY = (noseBaseLm.y - box.top) / faceHeight;
            m.put("noseYRatio", noseY);
        }

        // ancho nariz por contorno inferior
        float noseWidth = contourWidth(noseBottom) / faceWidth;
        if (noseWidth > 0) m.put("noseWidthRatio", noseWidth);

        // ancho rostro por contorno facial
        float contourFaceWidth = contourWidth(faceContour) / faceWidth;
        float contourFaceHeight = contourHeight(faceContour) / faceHeight;
        if (contourFaceWidth > 0) m.put("contourFaceWidth", contourFaceWidth);
        if (contourFaceHeight > 0) m.put("contourFaceHeight", contourFaceHeight);

        // proporción del rostro
        float faceRatio = faceHeight / faceWidth;
        m.put("faceRatio", faceRatio);

        // apertura de ojo por contorno
        float leftEyeOpenRatio = eyeOpenRatio(leftEyeContour, faceHeight);
        float rightEyeOpenRatio = eyeOpenRatio(rightEyeContour, faceHeight);
        if (leftEyeOpenRatio > 0) m.put("leftEyeContourOpen", leftEyeOpenRatio);
        if (rightEyeOpenRatio > 0) m.put("rightEyeContourOpen", rightEyeOpenRatio);

        // grosor labial experimental
        float upperLipThickness = lipThickness(upperLipTop, upperLipBottom) / faceHeight;
        float lowerLipThickness = lipThickness(lowerLipTop, lowerLipBottom) / faceHeight;
        if (upperLipThickness > 0) m.put("upperLipThickness", upperLipThickness);
        if (lowerLipThickness > 0) m.put("lowerLipThickness", lowerLipThickness);

        // altura de cejas aproximada
        float leftBrowHeight = browHeight(leftEyebrowTop, leftEyebrowBottom) / faceHeight;
        float rightBrowHeight = browHeight(rightEyebrowTop, rightEyebrowBottom) / faceHeight;
        if (leftBrowHeight > 0) m.put("leftBrowHeight", leftBrowHeight);
        if (rightBrowHeight > 0) m.put("rightBrowHeight", rightBrowHeight);

        // ancho entre mejillas
        float cheekWidth = -1f;
        if (leftCheekLm != null && rightCheekLm != null) {
            cheekWidth = dist(leftCheekLm, rightCheekLm) / faceWidth;
            m.put("cheekWidthRatio", cheekWidth);
        } else {
            float lcX = contourCenterX(leftCheekContour);
            float rcX = contourCenterX(rightCheekContour);
            if (lcX >= 0 && rcX >= 0) {
                cheekWidth = Math.abs(rcX - lcX) / faceWidth;
                m.put("cheekWidthRatio", cheekWidth);
            }
        }

        // ancho entre orejas si existe
        float earDistance = -1f;
        if (leftEarLm != null && rightEarLm != null) {
            earDistance = dist(leftEarLm, rightEarLm) / faceWidth;
            m.put("earDistanceRatio", earDistance);
        }

        // simetría experimental
        float symmetry = approximateSymmetry(faceContour, box);
        m.put("symmetry", symmetry);

        // centralidad nariz
        if (noseBaseLm != null) {
            float centerX = box.centerX();
            float noseCentered = 1f - (Math.abs(noseBaseLm.x - centerX) / (faceWidth / 2f));
            m.put("noseCentered", clamp01(noseCentered));
        }

        // boca abierta
        float mouthOpen = -1f;
        if (mouthBottomLm != null && mouthLeftLm != null && mouthRightLm != null) {
            PointF mouthCenter = new PointF((mouthLeftLm.x + mouthRightLm.x) / 2f,
                    (mouthLeftLm.y + mouthRightLm.y) / 2f);
            mouthOpen = Math.abs(mouthBottomLm.y - mouthCenter.y) / faceHeight;
            m.put("mouthOpenRatio", mouthOpen);
        }

        // puente nariz
        float noseBridgeHeight = contourHeight(noseBridge) / faceHeight;
        if (noseBridgeHeight > 0) m.put("noseBridgeHeight", noseBridgeHeight);

        // cantidad total de puntos de contorno encontrados
        int totalContourPoints =
                size(faceContour) + size(leftEyeContour) + size(rightEyeContour)
                        + size(upperLipTop) + size(upperLipBottom)
                        + size(lowerLipTop) + size(lowerLipBottom)
                        + size(noseBridge) + size(noseBottom)
                        + size(leftEyebrowTop) + size(rightEyebrowTop)
                        + size(leftEyebrowBottom) + size(rightEyebrowBottom)
                        + size(leftCheekContour) + size(rightCheekContour);

        m.put("totalContourPoints", (float) totalContourPoints);

        // -------------------------
        // 7) RASGOS DERIVADOS
        // -------------------------
        // Rostro
        if (faceRatio >= 1.35f) {
            rasgos.add(new Rasgo("Rostro alargado"));
        } else if (faceRatio <= 1.15f) {
            rasgos.add(new Rasgo("Rostro ancho"));
        } else {
            rasgos.add(new Rasgo("Rostro equilibrado"));
        }

        // Ojos
        if (eyeDistance > 0) {
            if (eyeDistance > 0.34f) {
                rasgos.add(new Rasgo("Ojos relativamente separados"));
            } else if (eyeDistance < 0.26f) {
                rasgos.add(new Rasgo("Ojos relativamente juntos"));
            } else {
                rasgos.add(new Rasgo("Separación ocular media"));
            }
        }

        float avgEyeOpen = avg(leftEyeOpenRatio, rightEyeOpenRatio);
        if (avgEyeOpen > 0) {
            if (avgEyeOpen > 0.030f) {
                rasgos.add(new Rasgo("Apertura ocular alta"));
            } else if (avgEyeOpen < 0.018f) {
                rasgos.add(new Rasgo("Apertura ocular baja"));
            } else {
                rasgos.add(new Rasgo("Apertura ocular media"));
            }
        }

        // Mejillas / ancho medio
        if (cheekWidth > 0) {
            if (cheekWidth > 0.55f) {
                rasgos.add(new Rasgo("Zona media del rostro ancha"));
            } else if (cheekWidth < 0.42f) {
                rasgos.add(new Rasgo("Zona media del rostro estrecha"));
            } else {
                rasgos.add(new Rasgo("Zona media del rostro equilibrada"));
            }
        }

        // Nariz
        if (noseWidth > 0) {
            if (noseWidth > 0.20f) {
                rasgos.add(new Rasgo("Base nasal amplia"));
            } else if (noseWidth < 0.12f) {
                rasgos.add(new Rasgo("Base nasal estrecha"));
            } else {
                rasgos.add(new Rasgo("Base nasal media"));
            }
        }

        // Boca / labios
        if (mouthWidth > 0) {
            if (mouthWidth > 0.34f) {
                rasgos.add(new Rasgo("Boca ancha"));
            } else if (mouthWidth < 0.24f) {
                rasgos.add(new Rasgo("Boca estrecha"));
            } else {
                rasgos.add(new Rasgo("Boca de ancho medio"));
            }
        }

        float avgLip = avg(upperLipThickness, lowerLipThickness);
        if (avgLip > 0) {
            if (avgLip > 0.030f) {
                rasgos.add(new Rasgo("Labios más notorios"));
            } else if (avgLip < 0.018f) {
                rasgos.add(new Rasgo("Labios más finos"));
            } else {
                rasgos.add(new Rasgo("Labios de grosor medio"));
            }
        }

        // Sonrisa
        if (sonrisa >= 0.70f) {
            rasgos.add(new Rasgo("Expresión sonriente"));
        } else if (sonrisa <= 0.20f) {
            rasgos.add(new Rasgo("Expresión seria"));
        } else {
            rasgos.add(new Rasgo("Expresión neutra"));
        }

        // Cejas
        float avgBrow = avg(leftBrowHeight, rightBrowHeight);
        if (avgBrow > 0) {
            if (avgBrow > 0.020f) {
                rasgos.add(new Rasgo("Cejas más elevadas"));
            } else {
                rasgos.add(new Rasgo("Cejas cercanas a los ojos"));
            }
        }

        // Simetría
        if (symmetry >= 0.75f) {
            rasgos.add(new Rasgo("Simetría facial alta"));
        } else if (symmetry >= 0.50f) {
            rasgos.add(new Rasgo("Simetría facial media"));
        } else {
            rasgos.add(new Rasgo("Simetría facial variable"));
        }

        // Orientación
        if (Math.abs(rotY) > 12f) {
            rasgos.add(new Rasgo("Rostro girado lateralmente"));
        }
        if (Math.abs(rotZ) > 10f) {
            rasgos.add(new Rasgo("Cabeza inclinada"));
        }
        if (Math.abs(rotX) > 10f) {
            rasgos.add(new Rasgo("Rostro inclinado verticalmente"));
        }

        // Tracking
        if (tracking != -1) {
            rasgos.add(new Rasgo("Tracking detectado"));
        }

        // Calidad experimental
        if (totalContourPoints >= 100) {
            rasgos.add(new Rasgo("Detección detallada de contornos"));
        } else {
            rasgos.add(new Rasgo("Detección parcial de contornos"));
        }

        // -------------------------
        // 8) PUNTAJES DE 4 PERFILES
        // -------------------------
        float p1 = 0f, w1 = 0f;
        float p2 = 0f, w2 = 0f;
        float p3 = 0f, w3 = 0f;
        float p4 = 0f, w4 = 0f;
        float p5 = 0f, w5 = 0f;
        float p6 = 0f, w6 = 0f;
        float p7 = 0f, w7 = 0f;
        float p8 = 0f, w8 = 0f;
        float p9 = 0f, w9 = 0f;
        float p10 = 0f, w10 = 0f;
        float p11 = 0f, w11 = 0f;
        float p12 = 0f, w12 = 0f;
        float p13 = 0f, w13 = 0f;
        float p14 = 0f, w14 = 0f;
        float p15 = 0f, w15 = 0f;
        float p16 = 0f, w16 = 0f;
        float p17 = 0f, w17 = 0f;
        float p18 = 0f, w18 = 0f;
        float p19 = 0f, w19 = 0f;
        float p20 = 0f, w20 = 0f;
        float p21 = 0f, w21 = 0f;
        float p22 = 0f, w22 = 0f;
        float p23 = 0f, w23 = 0f;
        float p24 = 0f, w24 = 0f;

        // PERFIL 1
        p1 += scoreHigh(faceRatio, 1.20f, 1.55f) * 1.2f; w1 += 1.2f;
        p1 += scoreLow(noseWidth, 0.11f, 0.17f) * 1.1f; w1 += 1.1f;
        p1 += scoreMid(mouthWidth, 0.24f, 0.31f, 0.36f) * 1.0f; w1 += 1.0f;
        p1 += scoreHigh(symmetry, 0.55f, 0.90f) * 1.0f; w1 += 1.0f;
        p1 += scoreMid(avgEyeOpen, 0.016f, 0.022f, 0.030f) * 0.9f; w1 += 0.9f;

// PERFIL 2
        p2 += scoreLow(faceRatio, 1.08f, 1.22f) * 1.0f; w2 += 1.0f;
        p2 += scoreHigh(cheekWidth, 0.46f, 0.58f) * 1.0f; w2 += 1.0f;
        p2 += scoreMid(noseWidth, 0.13f, 0.16f, 0.19f) * 0.9f; w2 += 0.9f;
        p2 += scoreHigh(sonrisa, 0.35f, 0.80f) * 0.8f; w2 += 0.8f;
        p2 += scoreMid(mouthWidth, 0.27f, 0.31f, 0.35f) * 0.8f; w2 += 0.8f;

// PERFIL 3
        p3 += scoreHigh(eyeDistance, 0.26f, 0.36f) * 1.1f; w3 += 1.1f;
        p3 += scoreHigh(mouthWidth, 0.25f, 0.38f) * 1.1f; w3 += 1.1f;
        p3 += scoreHigh(avgLip, 0.016f, 0.036f) * 1.0f; w3 += 1.0f;
        p3 += scoreMid(faceRatio, 1.10f, 1.24f, 1.40f) * 0.9f; w3 += 0.9f;
        p3 += scoreMid(avgEyeOpen, 0.018f, 0.026f, 0.034f) * 0.9f; w3 += 0.9f;

// PERFIL 4
        p4 += scoreLow(faceRatio, 1.00f, 1.22f) * 1.0f; w4 += 1.0f;
        p4 += scoreHigh(noseWidth, 0.13f, 0.24f) * 1.0f; w4 += 1.0f;
        p4 += scoreLow(sonrisa, 0.05f, 0.55f) * 0.9f; w4 += 0.9f;
        p4 += scoreHigh(Math.abs(rotZ), 0f, 18f) * 0.7f; w4 += 0.7f;
        p4 += scoreHigh(earDistance, 0.70f, 1.10f) * 0.7f; w4 += 0.7f;

// PERFIL 5
        p5 += scoreHigh(faceRatio, 1.25f, 1.60f) * 1.1f; w5 += 1.1f;
        p5 += scoreHigh(eyeDistance, 0.27f, 0.40f) * 1.1f; w5 += 1.1f;
        p5 += scoreMid(mouthWidth, 0.25f, 0.32f, 0.38f) * 1.0f; w5 += 1.0f;
        p5 += scoreHigh(symmetry, 0.55f, 0.90f) * 0.9f; w5 += 0.9f;
        p5 += scoreMid(avgEyeOpen, 0.018f, 0.026f, 0.034f) * 0.8f; w5 += 0.8f;

// PERFIL 6
        p6 += scoreMid(faceRatio, 1.10f, 1.25f, 1.40f) * 1.1f; w6 += 1.1f;
        p6 += scoreMid(noseWidth, 0.12f, 0.17f, 0.22f) * 1.1f; w6 += 1.1f;
        p6 += scoreHigh(symmetry, 0.60f, 0.95f) * 1.0f; w6 += 1.0f;
        p6 += scoreMid(mouthWidth, 0.24f, 0.30f, 0.36f) * 0.9f; w6 += 0.9f;
        p6 += scoreMid(eyeDistance, 0.25f, 0.30f, 0.36f) * 0.8f; w6 += 0.8f;

// PERFIL 7
        p7 += scoreHigh(mouthWidth, 0.28f, 0.40f) * 1.1f; w7 += 1.1f;
        p7 += scoreHigh(sonrisa, 0.30f, 0.90f) * 1.0f; w7 += 1.0f;
        p7 += scoreMid(avgLip, 0.015f, 0.025f, 0.035f) * 1.0f; w7 += 1.0f;
        p7 += scoreHigh(avgEyeOpen, 0.02f, 0.05f) * 0.8f; w7 += 0.8f;
        p7 += scoreMid(faceRatio, 1.10f, 1.28f, 1.40f) * 0.8f; w7 += 0.8f;

// PERFIL 8
        p8 += scoreLow(faceRatio, 1.00f, 1.20f) * 1.0f; w8 += 1.0f;
        p8 += scoreHigh(noseWidth, 0.14f, 0.28f) * 1.0f; w8 += 1.0f;
        p8 += scoreHigh(cheekWidth, 0.40f, 0.65f) * 1.0f; w8 += 1.0f;
        p8 += scoreLow(sonrisa, 0.05f, 0.55f) * 0.8f; w8 += 0.8f;
        p8 += scoreHigh(symmetry, 0.55f, 0.90f) * 0.8f; w8 += 0.8f;

// PERFIL 9
        p9 += scoreLow(eyeDistance, 0.20f, 0.28f) * 1.1f; w9 += 1.1f;
        p9 += scoreHigh(faceRatio, 1.25f, 1.60f) * 1.0f; w9 += 1.0f;
        p9 += scoreHigh(symmetry, 0.55f, 0.90f) * 1.0f; w9 += 1.0f;
        p9 += scoreLow(noseWidth, 0.10f, 0.18f) * 0.9f; w9 += 0.9f;
        p9 += scoreMid(mouthWidth, 0.24f, 0.30f, 0.36f) * 0.8f; w9 += 0.8f;

// PERFIL 10
        p10 += scoreMid(mouthWidth, 0.24f, 0.30f, 0.36f) * 1.1f; w10 += 1.1f;
        p10 += scoreMid(noseWidth, 0.12f, 0.18f, 0.24f) * 1.0f; w10 += 1.0f;
        p10 += scoreMid(faceRatio, 1.10f, 1.25f, 1.40f) * 1.0f; w10 += 1.0f;
        p10 += scoreMid(eyeDistance, 0.25f, 0.30f, 0.36f) * 0.9f; w10 += 0.9f;
        p10 += scoreHigh(symmetry, 0.60f, 0.95f) * 0.8f; w10 += 0.8f;

// PERFIL 11
        p11 += scoreLow(faceRatio, 1.00f, 1.20f) * 1.0f; w11 += 1.0f;
        p11 += scoreHigh(avgEyeOpen, 0.02f, 0.04f) * 1.1f; w11 += 1.1f;
        p11 += scoreHigh(cheekWidth, 0.40f, 0.60f) * 1.0f; w11 += 1.0f;
        p11 += scoreMid(noseWidth, 0.12f, 0.18f, 0.24f) * 0.8f; w11 += 0.8f;
        p11 += scoreHigh(symmetry, 0.55f, 0.90f) * 0.8f; w11 += 0.8f;

// PERFIL 12
        p12 += scoreHigh(sonrisa, 0.40f, 0.90f) * 1.1f; w12 += 1.1f;
        p12 += scoreHigh(avgLip, 0.02f, 0.04f) * 1.0f; w12 += 1.0f;
        p12 += scoreMid(mouthWidth, 0.26f, 0.32f, 0.38f) * 1.0f; w12 += 1.0f;
        p12 += scoreHigh(avgEyeOpen, 0.02f, 0.05f) * 0.8f; w12 += 0.8f;
        p12 += scoreMid(faceRatio, 1.10f, 1.28f, 1.40f) * 0.8f; w12 += 0.8f;

// PERFIL 13
        p13 += scoreLow(noseWidth, 0.10f, 0.18f) * 1.1f; w13 += 1.1f;
        p13 += scoreHigh(faceRatio, 1.30f, 1.65f) * 1.0f; w13 += 1.0f;
        p13 += scoreHigh(symmetry, 0.60f, 0.95f) * 1.0f; w13 += 1.0f;
        p13 += scoreLow(eyeDistance, 0.20f, 0.28f) * 0.8f; w13 += 0.8f;
        p13 += scoreMid(mouthWidth, 0.24f, 0.30f, 0.36f) * 0.8f; w13 += 0.8f;

// PERFIL 14
        p14 += scoreMid(faceRatio, 1.10f, 1.25f, 1.40f) * 1.1f; w14 += 1.1f;
        p14 += scoreMid(eyeDistance, 0.25f, 0.30f, 0.36f) * 1.0f; w14 += 1.0f;
        p14 += scoreMid(avgEyeOpen, 0.018f, 0.026f, 0.034f) * 1.0f; w14 += 1.0f;
        p14 += scoreMid(noseWidth, 0.12f, 0.18f, 0.24f) * 0.8f; w14 += 0.8f;
        p14 += scoreHigh(symmetry, 0.60f, 0.95f) * 0.8f; w14 += 0.8f;

// PERFIL 15
        p15 += scoreHigh(mouthWidth, 0.30f, 0.42f) * 1.1f; w15 += 1.1f;
        p15 += scoreHigh(avgLip, 0.02f, 0.04f) * 1.0f; w15 += 1.0f;
        p15 += scoreMid(symmetry, 0.50f, 0.70f, 0.90f) * 0.9f; w15 += 0.9f;
        p15 += scoreHigh(sonrisa, 0.25f, 0.85f) * 0.8f; w15 += 0.8f;
        p15 += scoreMid(faceRatio, 1.10f, 1.28f, 1.40f) * 0.8f; w15 += 0.8f;

// PERFIL 16
        p16 += scoreLow(faceRatio, 1.00f, 1.22f) * 1.0f; w16 += 1.0f;
        p16 += scoreMid(noseWidth, 0.12f, 0.18f, 0.25f) * 1.0f; w16 += 1.0f;
        p16 += scoreHigh(cheekWidth, 0.40f, 0.60f) * 1.0f; w16 += 1.0f;
        p16 += scoreLow(sonrisa, 0.05f, 0.55f) * 0.8f; w16 += 0.8f;
        p16 += scoreHigh(symmetry, 0.55f, 0.90f) * 0.8f; w16 += 0.8f;

// PERFIL 17
        p17 += scoreHigh(avgEyeOpen, 0.02f, 0.05f) * 1.1f; w17 += 1.1f;
        p17 += scoreMid(sonrisa, 0.20f, 0.50f, 0.80f) * 1.0f; w17 += 1.0f;
        p17 += scoreMid(faceRatio, 1.10f, 1.30f, 1.45f) * 1.0f; w17 += 1.0f;
        p17 += scoreMid(eyeDistance, 0.25f, 0.30f, 0.36f) * 0.8f; w17 += 0.8f;
        p17 += scoreHigh(symmetry, 0.60f, 0.95f) * 0.8f; w17 += 0.8f;

// PERFIL 18
        p18 += scoreHigh(noseWidth, 0.16f, 0.30f) * 1.0f; w18 += 1.0f;
        p18 += scoreHigh(cheekWidth, 0.45f, 0.65f) * 1.0f; w18 += 1.0f;
        p18 += scoreLow(faceRatio, 1.00f, 1.25f) * 1.0f; w18 += 1.0f;
        p18 += scoreLow(sonrisa, 0.05f, 0.55f) * 0.8f; w18 += 0.8f;
        p18 += scoreHigh(symmetry, 0.55f, 0.90f) * 0.8f; w18 += 0.8f;

// PERFIL 19
        p19 += scoreHigh(faceRatio, 1.30f, 1.60f) * 1.1f; w19 += 1.1f;
        p19 += scoreMid(mouthWidth, 0.24f, 0.30f, 0.36f) * 1.0f; w19 += 1.0f;
        p19 += scoreHigh(symmetry, 0.60f, 0.90f) * 1.0f; w19 += 1.0f;
        p19 += scoreLow(noseWidth, 0.10f, 0.18f) * 0.8f; w19 += 0.8f;
        p19 += scoreMid(avgEyeOpen, 0.018f, 0.026f, 0.034f) * 0.8f; w19 += 0.8f;

// PERFIL 20
        p20 += scoreLow(avgLip, 0.01f, 0.02f) * 1.0f; w20 += 1.0f;
        p20 += scoreMid(faceRatio, 1.10f, 1.25f, 1.40f) * 1.0f; w20 += 1.0f;
        p20 += scoreMid(mouthWidth, 0.24f, 0.30f, 0.36f) * 1.0f; w20 += 1.0f;
        p20 += scoreMid(noseWidth, 0.12f, 0.18f, 0.24f) * 0.8f; w20 += 0.8f;
        p20 += scoreHigh(symmetry, 0.60f, 0.95f) * 0.8f; w20 += 0.8f;

// PERFIL 21
        p21 += scoreHigh(eyeDistance, 0.27f, 0.40f) * 1.1f; w21 += 1.1f;
        p21 += scoreHigh(sonrisa, 0.40f, 0.90f) * 1.0f; w21 += 1.0f;
        p21 += scoreMid(faceRatio, 1.10f, 1.30f, 1.45f) * 1.0f; w21 += 1.0f;
        p21 += scoreHigh(avgEyeOpen, 0.02f, 0.05f) * 0.8f; w21 += 0.8f;
        p21 += scoreHigh(symmetry, 0.55f, 0.90f) * 0.8f; w21 += 0.8f;

// PERFIL 22
        p22 += scoreLow(faceRatio, 1.00f, 1.22f) * 1.0f; w22 += 1.0f;
        p22 += scoreHigh(avgLip, 0.02f, 0.04f) * 1.0f; w22 += 1.0f;
        p22 += scoreHigh(cheekWidth, 0.40f, 0.60f) * 1.0f; w22 += 1.0f;
        p22 += scoreHigh(sonrisa, 0.25f, 0.85f) * 0.8f; w22 += 0.8f;
        p22 += scoreMid(mouthWidth, 0.26f, 0.32f, 0.38f) * 0.8f; w22 += 0.8f;

// PERFIL 23
        p23 += scoreMid(noseWidth, 0.12f, 0.18f, 0.24f) * 1.1f; w23 += 1.1f;
        p23 += scoreMid(eyeDistance, 0.25f, 0.30f, 0.36f) * 1.0f; w23 += 1.0f;
        p23 += scoreHigh(symmetry, 0.60f, 0.95f) * 1.0f; w23 += 1.0f;
        p23 += scoreMid(faceRatio, 1.10f, 1.25f, 1.40f) * 0.8f; w23 += 0.8f;
        p23 += scoreMid(avgEyeOpen, 0.018f, 0.026f, 0.034f) * 0.8f; w23 += 0.8f;

// PERFIL 24
        p24 += scoreMid(faceRatio, 1.10f, 1.30f, 1.45f) * 1.0f; w24 += 1.0f;
        p24 += scoreMid(sonrisa, 0.20f, 0.50f, 0.80f) * 1.0f; w24 += 1.0f;
        p24 += scoreMid(mouthWidth, 0.25f, 0.32f, 0.38f) * 1.0f; w24 += 1.0f;
        p24 += scoreMid(noseWidth, 0.12f, 0.18f, 0.24f) * 0.8f; w24 += 0.8f;
        p24 += scoreHigh(symmetry, 0.55f, 0.90f) * 0.8f; w24 += 0.8f;

        // NORMALIZAR TODOS
        p1 /= w1;   p2 /= w2;   p3 /= w3;   p4 /= w4;
        p5 /= w5;   p6 /= w6;   p7 /= w7;   p8 /= w8;
        p9 /= w9;   p10 /= w10; p11 /= w11; p12 /= w12;
        p13 /= w13; p14 /= w14; p15 /= w15; p16 /= w16;
        p17 /= w17; p18 /= w18; p19 /= w19; p20 /= w20;
        p21 /= w21; p22 /= w22; p23 /= w23; p24 /= w24;
        // empate / robustez con simetría y puntos
        float detalle = normalize(totalContourPoints, 40f, 130f);
        p1 += detalle * 0.05f;
        p2 += detalle * 0.05f;
        p3 += detalle * 0.05f;
        p4 += detalle * 0.05f;
        p5 += detalle * 0.05f;
        p6 += detalle * 0.05f;
        p7 += detalle * 0.05f;
        p8 += detalle * 0.05f;
        p9 += detalle * 0.05f;
        p10 += detalle * 0.05f;
        p11 += detalle * 0.05f;
        p12 += detalle * 0.05f;
        p13 += detalle * 0.05f;
        p14 += detalle * 0.05f;
        p15 += detalle * 0.05f;
        p16 += detalle * 0.05f;
        p17 += detalle * 0.05f;
        p18 += detalle * 0.05f;
        p19 += detalle * 0.05f;
        p20 += detalle * 0.05f;
        p21 += detalle * 0.05f;
        p22 += detalle * 0.05f;
        p23 += detalle * 0.05f;
        p24 += detalle * 0.05f;


        // -------------------------
        // 9) DECISIÓN FINAL
        // -------------------------
        float[] scores = {
                p1,p2,p3,p4,
                p5,p6,p7,p8,
                p9,p10,p11,p12,p13,p14,
                p15,p16,p17,p18,p19,p20,p21,p22,p23,p24
        };

        for(int i = 0; i < scores.length; i++){
            scores[i] += Math.random() * 0.6;
        }

        String[] nombres = {
                "Awá",
                "Chachi",
                "Épera",
                "Tsáchila",
                "Otavalo",
                "Cayambi",
                "Kitu Kara",
                "Panzaleo",
                "Chibuleo",
                "Salasaka",
                "Waranka",
                "Puruhá",
                "Kañari",
                "Saraguro",
                "Achuar",
                "Andoa",
                "Cofán",
                "Siona",
                "Secoya",
                "Shuar",
                "Shiwiar",
                "Waorani",
                "Zápara",
                "Kichwa Amazónico"
        };


        int idxMax = 0;
        float max = scores[0];
        float suma = 0f;

        for (int i = 0; i < scores.length; i++) {
            suma += scores[i];
            if (scores[i] > max) {
                max = scores[i];
                idxMax = i;
            }
        }

        float confianza;
        if (suma <= 0.0001f) {
            confianza = 0.25f;
        } else {
            confianza = (max - (suma / scores.length)) / max;
        }

        // Guardar puntajes también
        m.put("scorePerfil1", p1);
        m.put("scorePerfil2", p2);
        m.put("scorePerfil3", p3);
        m.put("scorePerfil4", p4);
        m.put("scorePerfil5", p5);
        m.put("scorePerfil6", p6);
        m.put("scorePerfil7", p7);
        m.put("scorePerfil8", p8);
        m.put("scorePerfil9", p9);
        m.put("scorePerfil10", p10);
        m.put("scorePerfil11", p11);
        m.put("scorePerfil12", p12);
        m.put("scorePerfil13", p13);
        m.put("scorePerfil14", p14);
        m.put("scorePerfil15", p15);
        m.put("scorePerfil16", p16);
        m.put("scorePerfil17", p17);
        m.put("scorePerfil18", p18);
        m.put("scorePerfil19", p19);
        m.put("scorePerfil20", p20);
        m.put("scorePerfil21", p21);
        m.put("scorePerfil22", p22);
        m.put("scorePerfil23", p23);
        m.put("scorePerfil24", p24);

        // Agregar resumen del perfil elegido
        rasgos.add(0, new Rasgo("Clasificación experimental: " + nombres[idxMax]));
        rasgos.add(1, new Rasgo(String.format(Locale.US,
                "Puntaje relativo: %.2f", max)));

        String region;

        if(idxMax <= 3){
            region = "Región Costa (Litoral)";
        }
        else if(idxMax <= 13){
            region = "Región Sierra (Andes)\nKichwa de la Sierra";
        }
        else{
            region = "Región Amazónica (Oriente)";
        }

        String perfilFinal =
                region + "\nPueblo/Nacionalidad: " + nombres[idxMax];

        if(confianza < 0.35f){
            perfilFinal = "Perfil no claro\n" + perfilFinal;
        }
        return new Resultado(perfilFinal, clamp01(confianza), rasgos, m);
    }

    // =========================
    // UTILIDADES
    // =========================
    private static Float safeProb(Float value) {
        if (value == null) return 0f;
        return clamp01(value);
    }

    private static PointF getLandmark(Face face, int type) {
        FaceLandmark lm = face.getLandmark(type);
        return (lm != null) ? lm.getPosition() : null;
    }

    private static List<PointF> getContour(Face face, int type) {
        FaceContour contour = face.getContour(type);
        if (contour == null || contour.getPoints() == null) {
            return new ArrayList<>();
        }
        return contour.getPoints();
    }

    private static float dist(PointF a, PointF b) {
        float dx = a.x - b.x;
        float dy = a.y - b.y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private static int size(List<PointF> pts) {
        return pts == null ? 0 : pts.size();
    }

    private static float contourWidth(List<PointF> pts) {
        if (pts == null || pts.isEmpty()) return -1f;
        float minX = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        for (PointF p : pts) {
            minX = Math.min(minX, p.x);
            maxX = Math.max(maxX, p.x);
        }
        return maxX - minX;
    }

    private static float contourHeight(List<PointF> pts) {
        if (pts == null || pts.isEmpty()) return -1f;
        float minY = Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        for (PointF p : pts) {
            minY = Math.min(minY, p.y);
            maxY = Math.max(maxY, p.y);
        }
        return maxY - minY;
    }

    private static float contourCenterX(List<PointF> pts) {
        if (pts == null || pts.isEmpty()) return -1f;
        float sum = 0f;
        for (PointF p : pts) sum += p.x;
        return sum / pts.size();
    }

    private static float eyeOpenRatio(List<PointF> eyeContour, float faceHeight) {
        if (eyeContour == null || eyeContour.isEmpty()) return -1f;
        float h = contourHeight(eyeContour);
        if (h <= 0) return -1f;
        return h / faceHeight;
    }

    private static float lipThickness(List<PointF> a, List<PointF> b) {
        if (a == null || b == null || a.isEmpty() || b.isEmpty()) return -1f;
        int n = Math.min(a.size(), b.size());
        if (n == 0) return -1f;

        float sum = 0f;
        for (int i = 0; i < n; i++) {
            sum += Math.abs(a.get(i).y - b.get(i).y);
        }
        return sum / n;
    }

    private static float browHeight(List<PointF> top, List<PointF> bottom) {
        if (top == null || bottom == null || top.isEmpty() || bottom.isEmpty()) return -1f;
        int n = Math.min(top.size(), bottom.size());
        if (n == 0) return -1f;

        float sum = 0f;
        for (int i = 0; i < n; i++) {
            sum += Math.abs(bottom.get(i).y - top.get(i).y);
        }
        return sum / n;
    }

    private static float approximateSymmetry(List<PointF> faceContour, Rect box) {
        if (faceContour == null || faceContour.size() < 4) return 0.5f;

        float centerX = box.centerX();
        float acc = 0f;
        int count = 0;

        for (PointF p : faceContour) {
            float d = Math.abs(p.x - centerX) / (box.width() / 2f);
            acc += d;
            count++;
        }

        if (count == 0) return 0.5f;

        float avg = acc / count;
        float symmetry = 1f - Math.abs(avg - 0.5f);
        return clamp01(symmetry);
    }

    private static float avg(float a, float b) {
        if (a < 0 && b < 0) return -1f;
        if (a < 0) return b;
        if (b < 0) return a;
        return (a + b) / 2f;
    }

    private static float clamp01(float v) {
        if (v < 0f) return 0f;
        return Math.min(v, 1f);
    }

    private static float normalize(float value, float min, float max) {
        if (value < 0) return 0f;
        if (max <= min) return 0f;
        float n = (value - min) / (max - min);
        return clamp01(n);
    }

    private static float scoreHigh(float value, float min, float max) {
        if (value < 0) return 0f;
        return normalize(value, min, max);
    }

    private static float scoreLow(float value, float min, float max) {
        if (value < 0) return 0f;
        float n = normalize(value, min, max);
        return 1f - n;
    }

    private static float scoreMid(float value, float low, float ideal, float high) {
        if (value < 0) return 0f;

        if (value <= low || value >= high) return 0f;
        if (value == ideal) return 1f;

        if (value < ideal) {
            return (value - low) / (ideal - low);
        } else {
            return (high - value) / (high - ideal);
        }
    }

}
