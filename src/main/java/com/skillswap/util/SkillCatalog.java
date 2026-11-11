package com.skillswap.util;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Skill names and categories. Gives every skill one canonical spelling (so "java", "JAVA " and "Java"
 * are the same skill) and a category used by the category-wise leaderboards ("Top Programming Mentor").
 */
public final class SkillCatalog {
    private SkillCatalog() {}

    public static final String GENERAL = "General";

    /** Categories shown in dropdowns, in display order. */
    public static final List<String> CATEGORIES = Arrays.asList(
            "Programming", "Data & AI", "Web Development", "Design", "Languages",
            "Academics", "Music & Arts", "Sports & Fitness", "Soft Skills", GENERAL);

    // canonical name (lower case key) -> {Display Name, Category}
    private static final Map<String, String[]> KNOWN = new LinkedHashMap<>();

    private static void add(String category, String... names) {
        for (String n : names) KNOWN.put(n.toLowerCase(), new String[]{n, category});
    }

    static {
        add("Programming", "Java", "Python", "C", "C++", "C#", "Go", "Rust", "Kotlin", "Swift", "PHP", "Ruby",
                "Data Structures", "Algorithms", "Competitive Programming", "Spring Boot", "Git", "Linux", "DSA", "OOP");
        add("Web Development", "HTML", "CSS", "JavaScript", "TypeScript", "React", "Angular", "Node.js", "Vue",
                "Web Development", "JSF", "Bootstrap", "Tailwind CSS");
        add("Data & AI", "Machine Learning", "Deep Learning", "Data Science", "Data Analysis", "SQL", "MySQL",
                "MongoDB", "Excel", "Power BI", "Tableau", "Statistics", "NLP", "Computer Vision", "AI");
        add("Design", "Figma", "UI/UX", "UI Design", "UX Design", "Photoshop", "Illustrator", "Canva",
                "Graphic Design", "Video Editing", "Animation", "Photography");
        add("Languages", "English", "Hindi", "Kannada", "Tamil", "Telugu", "French", "German", "Spanish",
                "Japanese", "Korean");
        add("Academics", "Mathematics", "Maths", "Physics", "Chemistry", "Engineering Mechanics", "Digital Electronics",
                "Operating Systems", "DBMS", "Computer Networks", "Aptitude");
        add("Music & Arts", "Guitar", "Piano", "Singing", "Drawing", "Painting", "Dance", "Keyboard", "Drums");
        add("Sports & Fitness", "Chess", "Cricket", "Football", "Badminton", "Yoga", "Gym", "Swimming");
        add("Soft Skills", "Public Speaking", "Communication", "Leadership", "Resume Writing", "Interview Preparation",
                "Time Management", "Content Writing");
    }

    /** Trim, collapse spaces and use the well-known spelling if there is one. */
    public static String normalizeName(String raw) {
        if (raw == null) return "";
        String name = raw.trim().replaceAll("\\s+", " ");
        if (name.isEmpty()) return "";
        String[] known = KNOWN.get(name.toLowerCase());
        if (known != null) return known[0];
        // all lower case input -> Title Case ("machine design" -> "Machine Design")
        if (name.equals(name.toLowerCase())) {
            StringBuilder sb = new StringBuilder();
            for (String w : name.split(" ")) {
                if (sb.length() > 0) sb.append(' ');
                sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1).toLowerCase());
            }
            return sb.toString();
        }
        return name;
    }

    /** Category for a skill name: a known skill's category, or General. */
    public static String categoryFor(String name) {
        String[] known = KNOWN.get(normalizeName(name).toLowerCase());
        return known != null ? known[1] : GENERAL;
    }

    /** Accepts a category from a dropdown; anything unknown becomes General. */
    public static String cleanCategory(String category) {
        if (category == null) return null;
        for (String c : CATEGORIES) if (c.equalsIgnoreCase(category.trim())) return c;
        return null;
    }
}
