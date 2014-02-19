package utils;

public class levenshteinDistance {

    /**
     * Funzione che restituice il minimo di tre valori.
     *
     * @param int a
     * @param int b
     * @param int c
     * @return minimo dei valori tra a, b, c
     */
    private static int minimum(int a, int b, int c) {
        return Math.min(Math.min(a, b), c);
    }

    /**
     * Funzione che restituisce la distanza tra due parole
     *
     * @param stringa1
     * @param stringa2
     * @return distanza tra le parole
     */
    public int computeLevenshteinDistance(String stringa1, String stringa2) {

        if (stringa1 != null && stringa2 != null) {
            stringa1 = stringa1.trim().toLowerCase();
            stringa2 = stringa2.trim().toLowerCase();
            int stringa1Length = stringa1.length();
            int stringa2Length = stringa2.length();

            /**
             * Definisco una matrice di zeri NxM dove N ? la lunghezza della
             * prima parola e M ? la lunghezza della seconda parola
             */
            int[][] distance = new int[stringa1Length + 1][stringa2Length + 1];

            /**
             * Alla prima riga e alla prima colonna associo valori numerici
             * crescenti ( 0, 1, 2, 3...)
             */
            for (int i = 0; i <= stringa1Length; i++) {
                distance[i][0] = i;
            }
            for (int j = 1; j <= stringa2Length; j++) {
                distance[0][j] = j;
            }

            /**
             * Per ogni valore nella matrice in [i,j] definisco il minimo tra: 1
             * - [i-1, j] + 1 2 - [i, j-1] + 1 3 - [i-1, j-1] + (0 se i
             * caratteri sono uguali; 1 se i caratteri sono diversi) Il valore
             * trovato lo sostituisco alla posizione [i,j].
             */
            for (int i = 1; i <= stringa1Length; i++) {
                for (int j = 1; j <= stringa2Length; j++) {
                    distance[i][j] = minimum(
                            distance[i - 1][j] + 1,
                            distance[i][j - 1] + 1,
                            distance[i - 1][j - 1] + ((stringa1.charAt(i - 1) == stringa2.charAt(j - 1)) ? 0 : 1));
                }
            }

            return distance[stringa1Length][stringa2Length];
        } else {
            return -1;
        }
    }
}