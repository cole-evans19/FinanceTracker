import java.time.LocalDate;

public class Shift {
    private LocalDate date;
    private ShiftType type;      
    private double hours;
    private double wage;
    private double cashTips;
    private double cardTips;

    public Shift(LocalDate date, ShiftType type, double hours, double wage, double cashTips, double cardTips) {
        this.date = date;
        this.type = type;
        this.hours = hours;
        this.wage = wage;
        this.cashTips = cashTips;
        this.cardTips = cardTips;
    }

    public double getGrossPay() {
        return (hours * wage) + cashTips + cardTips;
    }

    public double getTotalTips() {
        return cashTips + cardTips;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public ShiftType getType() {
        return type;
    }

    public void setType(ShiftType type) {
        this.type = type;
    }

    public double getHours() {
        return hours;
    }

    public void setHours(double hours) {
        this.hours = hours;
    }

    public double getWage() {
        return wage;
    }

    public void setWage(double wage) {
        this.wage = wage;
    }

    public double getCashTips() {
        return cashTips;
    }

    public void setCashTips(double cashTips) {
        this.cashTips = cashTips;
    }

    public double getCardTips() {
        return cardTips;
    }

    public void setCardTips(double cardTips) {
        this.cardTips = cardTips;
    }

    
}